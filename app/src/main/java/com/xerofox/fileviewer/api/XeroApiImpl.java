package com.xerofox.fileviewer.api;

import android.arch.lifecycle.LiveData;
import android.os.Environment;

import com.elvishew.xlog.XLog;
import com.xerofox.fileviewer.AppExecutors;
import com.xerofox.fileviewer.helper.SettingHelper;
import com.xerofox.fileviewer.util.ByteBufferReader;
import com.xerofox.fileviewer.util.ByteBufferWriter;
import com.xerofox.fileviewer.util.FileUtil;
import com.xerofox.fileviewer.util.Logger;
import com.xerofox.fileviewer.util.XmlUtil;
import com.xerofox.fileviewer.vo.Resource;
import com.xerofox.fileviewer.vo.Task;
import com.xerofox.fileviewer.vo.TowerPart;

import org.kobjects.base64.Base64;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.MarshalBase64;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class XeroApiImpl implements XeroApi {
    private static String IP = SettingHelper.getServerPort();
    private static final String NAMESPACE = "http://xerofox.com/TMSService/";
    private static String URL = "http://" + SettingHelper.getServerPort() + "/TMSServ/TMSService.asmx";
    private static HttpTransportSE ht = null;
    private static int sessionId = 0;

    private static final String ERROR_NETWORK = "网络错误，请查看网络";
    private static final String ERROR_NO_DATA = "无数据";
    private static final String ERROR_NETWORK_TIMEOUT = "服务器连接超时";


    @Inject
    public XeroApiImpl() {
    }

    @Override
    public LiveData<ApiResponse<List<Task>>> loadAllTasks() {
        //  mock server api
        return new LiveData<ApiResponse<List<Task>>>() {
            @Override
            protected void onActive() {
                super.onActive();
                try {
                    List<Task> tasks = new ArrayList<>();
                    String directory = Environment.getExternalStorageDirectory().getPath() + File.separator + FileHelper.PATH_DATA_ROOT;
                    List<File> files = FileUtil.listFilesInDir(directory);
                    if (files == null || files.isEmpty()) {
                        ApiResponse<List<Task>> response = new ApiResponse<>(500, tasks, "没有本地文件");
                        postValue(response);
                    }
                    for (File file : files) {
                        RandomAccessFile raf;
                        raf = new RandomAccessFile(file, "r");
                        byte[] byteArr = new byte[(int) raf.length()];
                        raf.read(byteArr);
                        raf.close();
                        Task task = new Task(new ByteBufferReader(byteArr));
                        tasks.add(task);
                    }
                    ApiResponse<List<Task>> response = new ApiResponse<>(200, tasks, null);
                    postValue(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public LiveData<Resource<List<Task>>> getServerTasks(AppExecutors appExecutors) {
        return new LiveData<Resource<List<Task>>>() {
            @Override
            protected void onActive() {
                super.onActive();
                appExecutors.networkIO().execute(() -> {
                    List<Task> response = null;
                    try {
                        response = downloadSimpleTaskListFromServer(new int[0], false);
                        postValue(Resource.success(response));
                    } catch (Exception e) {
                        e.printStackTrace();
                        postValue(Resource.error(e.getMessage(), response));
                    }
                });
            }
        };
    }

    @Override
    public LiveData<ApiResponse<List<Task>>> getServerTasks(AppExecutors appExecutors, int[] localTaskIds) {
        return new LiveData<ApiResponse<List<Task>>>() {
            @Override
            protected void onActive() {
                super.onActive();
                XLog.i("XeroApiImpl getServerTasks onActive");
                appExecutors.networkIO().execute(() -> {
                    List<Task> response = null;
                    try {
                        response = downloadSimpleTaskListFromServer(localTaskIds, false);
                        postValue(new ApiResponse<>(response));
                    } catch (Exception e) {
                        e.printStackTrace();
                        postValue(new ApiResponse<>(500, response, e.getMessage()));
//                        postValue(Resource.error(e.getMessage(), response));
                    }
                });
            }
        };
    }

    @Override
    public LiveData<Resource<List<Task>>> downloadTasks(AppExecutors appExecutors, FileHelper fileHelper, List<Task> data) {
        return new LiveData<Resource<List<Task>>>() {
            @Override
            protected void onActive() {
                super.onActive();
                XLog.i("XeroApiImpl downloadTasks");
                appExecutors.networkIO().execute(() -> {
                    try {
                        List<Task> downloadedTask = new ArrayList<>();
                        for (int i = 0; i < data.size(); i++) {
                            Task task = data.get(i);
                            byte[] byteArr = downloadServerObject(task.getId(), "ManuElemTask");
                            if (byteArr == null)
                                continue;
                            ByteBufferReader br = new ByteBufferReader(byteArr);
                            task = new Task(br);
                            downloadedTask.add(task);
                        }
                        appExecutors.diskIO().execute(() -> {
                            fileHelper.saveTasks(downloadedTask);
                            postValue(Resource.success(downloadedTask));
                        });
                    } catch (Exception e) {
                        postValue(Resource.error(e.getMessage(), null));
                    }
                });
            }
        };
    }

    @Override
    public LiveData<Resource<List<Integer>>> checkUpdate(AppExecutors appExecutors, int id, List<TowerPart> parts) {
        return new LiveData<Resource<List<Integer>>>() {
            @Override
            protected void onActive() {
                super.onActive();
                appExecutors.networkIO().execute(() -> {
                    List<Integer> list = new ArrayList<>();
                    try {
                        list = queryTaskPartsUpdateState(id, parts);
                        postValue(Resource.success(list));
                    } catch (Exception e) {
                        e.printStackTrace();
                        postValue(Resource.error(e.getMessage(), list));
                    }
                });
            }
        };
    }

    @Override
    public LiveData<Resource<Boolean>> downloadTowerParts(AppExecutors appExecutors, FileHelper fileHelper, Task task, List<Integer> idArray) {
        return new LiveData<Resource<Boolean>>() {
            @Override
            protected void onActive() {
                super.onActive();
                appExecutors.networkIO().execute(() -> {
                    try {
                        List<TowerPart> towerParts = queryTaskParts(task.getId(), idArray);
                        appExecutors.diskIO().execute(() -> {
                            fileHelper.saveUpdateParts(task, towerParts);
                            postValue(Resource.success(true));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        postValue(Resource.error(e.getMessage(), false));
                    }
                });
            }
        };
    }

    private List<Task> downloadSimpleTaskListFromServer(int[] localTaskIdArr, boolean queryState) throws Exception {
        try {
            getSessionId();
            List<Task> taskList = new ArrayList<>();
            SoapObject rpc = new SoapObject(NAMESPACE, "QueryObjects");
            rpc.addProperty("sessionId", sessionId);
            rpc.addProperty("clsName", "ManuElemTask");
            XmlUtil xmlHelper = new XmlUtil();
            xmlHelper.AppendValue("IdArr", "id", localTaskIdArr);
            if (queryState)  //查询指定任务状态
                xmlHelper.AppendValue("QueryType", 1);
            rpc.addProperty("xmlScope", xmlHelper.ToXml());
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (ht == null)
                ht = new HttpTransportSE(URL, 100000);
            ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                throw new RuntimeException(ERROR_NO_DATA);
            }
            String retString = String.valueOf(ret);
            byte[] retByteArr = Base64.decode(retString);
            ByteBufferReader br = new ByteBufferReader(retByteArr);
            int taskArrLen = br.readInt();
            for (int i = 0; i < taskArrLen; i++) {
                int id = br.readInt();
                String name = br.readString();
                Task task = new Task(id, name);
                task.setDate(br.readDate());
                if (queryState) {
                    task.setNeedUpdate(br.readInt() > 0);
                } else {
                    task.setState(br.readInt());
                }
                taskList.add(task);
            }
            return taskList;
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl downloadSimpleTaskListFromServer SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl downloadSimpleTaskListFromServer Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    private int getSessionId() throws Exception {
        if (sessionId > 0) {
            XLog.i("XeroApiImpl getSessionId" + sessionId);
            return sessionId;
        } else {
            int id = loginUser(SettingHelper.getUserName(), SettingHelper.getPassword());
            XLog.i("XeroApiImpl getSessionId" + id);
            return id;
        }
    }

    private int loginUser(String userName, String password) throws Exception {
        try {
            XLog.i("XeroApiImpl downloadServerObject loginUser" + userName + password);
            sessionId = 0;
            SoapObject rpc = new SoapObject(NAMESPACE, "loginUser");
            rpc.addProperty("userName", userName);
            rpc.addProperty("password", password);
            rpc.addProperty("fingerprint", null);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (ht == null)
                ht = new HttpTransportSE(URL, 100000);
            ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                throw new RuntimeException(ERROR_NO_DATA);
            }
            sessionId = Integer.valueOf(String.valueOf(ret));
            return sessionId;
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl loginUser SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl loginUser Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    private byte[] downloadServerObject(int objId, String cls_name) throws Exception {
        try {
            XLog.i("XeroApiImpl downloadServerObject");
            int fileObjId = 0;
            int uiFileDataLength = 0;
            int sessionId = getSessionId();
            String retXML = openServerObjectDataProvider(sessionId, objId, cls_name, false);
            if (retXML != null && retXML.length() > 0) {
                XmlUtil xmlQuery = new XmlUtil(retXML);
                String idStr = xmlQuery.GetValue("fileObjId");
                if (idStr != null && idStr.length() > 0)
                    fileObjId = Integer.valueOf(idStr);
                String sizeStr = xmlQuery.GetValue("size");
                if (sizeStr != null && sizeStr.length() > 0)
                    uiFileDataLength = Integer.valueOf(sizeStr);
            }
            if (uiFileDataLength == 0 || fileObjId <= 0)
                return null;
            int indexpos = 0;
            int uiLeastSize = uiFileDataLength;
            ByteBufferWriter bw = new ByteBufferWriter(new ByteArrayOutputStream(1000));
            while (uiLeastSize > 0) {
                int uiDownloadSize = Math.min(uiLeastSize, 0x500000);
                byte[] byteArr = downloadFileObject(sessionId, fileObjId, indexpos, uiDownloadSize, false);
                if (byteArr == null || byteArr.length < uiDownloadSize) {
                    throw new RuntimeException(ERROR_NO_DATA);
                }
                bw.write(byteArr);
                indexpos += uiDownloadSize;
                uiLeastSize -= uiDownloadSize;
            }
            closeFileObjectDataProvider(sessionId, fileObjId);
            if (uiLeastSize == 0)
                return bw.toByteArray();
            else {
                throw new RuntimeException("数据文件下载失败!");
            }
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl downloadServerObject SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl downloadServerObject Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    private byte[] downloadFileObject(int sessionId, int idFileObj, int startposition, int download_size, boolean compressed) throws Exception {
        try {
            long startTime = System.currentTimeMillis();
            Logger.d(String.valueOf(startTime));
            SoapObject rpc = new SoapObject(XeroNetApi.NAMESPACE, "DownloadFileObject");
            rpc.addProperty("sessionId", sessionId);
            rpc.addProperty("idFileObj", idFileObj);
            rpc.addProperty("startposition", startposition);
            rpc.addProperty("download_size", download_size);
            rpc.addProperty("compressed", compressed);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (XeroNetApi.ht == null)
                XeroNetApi.ht = new HttpTransportSE(XeroNetApi.URL, 100000);
            XeroNetApi.ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                throw new RuntimeException("没有需要下载的新任务!");
            }
            return Base64.decode(String.valueOf(ret));
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl downloadFileObject SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl downloadFileObject Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    private String openServerObjectDataProvider(int sessionId, int idObject, String cls_name, boolean compressed) throws Exception {
        try {
            SoapObject rpc = new SoapObject(XeroNetApi.NAMESPACE, "OpenServerObjectDataProvider");
            rpc.addProperty("sessionId", sessionId);
            rpc.addProperty("idObject", idObject);
            rpc.addProperty("cls_name", cls_name);
            rpc.addProperty("compressed", compressed);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (XeroNetApi.ht == null)
                XeroNetApi.ht = new HttpTransportSE(XeroNetApi.URL, 100000);
            XeroNetApi.ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                throw new RuntimeException("无数据");
            }
            return String.valueOf(ret);
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl openServerObjectDataProvider SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl openServerObjectDataProvider Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    private boolean closeFileObjectDataProvider(int sessionId, int fileObjId) throws Exception {
        try {
            SoapObject rpc = new SoapObject(XeroNetApi.NAMESPACE, "CloseFileObjectDataProvider");
            rpc.addProperty("sessionId", sessionId);
            rpc.addProperty("idFileObj", fileObjId);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (XeroNetApi.ht == null)
                XeroNetApi.ht = new HttpTransportSE(XeroNetApi.URL, 100000);
            XeroNetApi.ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                return false;
            }
            String retString = String.valueOf(ret);
            return Boolean.valueOf(retString);
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl closeFileObjectDataProvider SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl closeFileObjectDataProvider Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    private List<Integer> queryTaskPartsUpdateState(int id, List<TowerPart> parts) throws Exception {
        try {
            List<Integer> array = new ArrayList<>();
            if (parts == null || parts.isEmpty()) {
                return array;
            }
            getSessionId();
            SoapObject rpc = new SoapObject(NAMESPACE, "QueryObjects");
            rpc.addProperty("sessionId", sessionId);
            rpc.addProperty("clsName", "ManuElemTaskPartId");
            XmlUtil xmlHelper = new XmlUtil();
            xmlHelper.AppendValue("ManuElemTaskId", id);
            xmlHelper.AppendValue("PartMd5Arr", parts);
            rpc.addProperty("xmlScope", xmlHelper.ToXml());
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (ht == null)
                ht = new HttpTransportSE(URL, 100000);
            ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                throw new RuntimeException(ERROR_NO_DATA);
            }
            String retString = String.valueOf(ret);
            byte[] retByteArr = Base64.decode(retString);
            ByteBufferReader br = new ByteBufferReader(retByteArr);
            int taskArrLen = br.readInt();
            for (int i = 0; i < taskArrLen; i++) {
                int partId = br.readInt();
                array.add(partId);
            }
            return array;
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl queryTaskPartsUpdateState SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl queryTaskPartsUpdateState Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    public List<TowerPart> queryTaskParts(int taskId, List<Integer> idList) throws Exception {
        try {
            List<TowerPart> partList = new ArrayList<>();
            if (idList == null || idList.isEmpty()) {
                return partList;
            }
            int[] idArray = new int[idList.size()];
            for (int i = 0; i < idList.size(); i++) {
                idArray[i] = idList.get(i);
            }
            getSessionId();
            SoapObject rpc = new SoapObject(NAMESPACE, "QueryObjects");
            rpc.addProperty("sessionId", sessionId);
            rpc.addProperty("clsName", "ManuElemTaskPart");
            XmlUtil xmlHelper = new XmlUtil();
            xmlHelper.AppendValue("ManuElemTaskId", taskId);
            xmlHelper.AppendValue("IdArr", "id", idArray);
            rpc.addProperty("xmlScope", xmlHelper.ToXml());
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.bodyOut = rpc;
            envelope.dotNet = true;
            new MarshalBase64().register(envelope);
            if (ht == null)
                ht = new HttpTransportSE(URL, 100000);
            ht.call(null, envelope);
            Object ret = envelope.getResponse();
            if (ret == null) {
                throw new RuntimeException(ERROR_NO_DATA);
            }
            String retString = String.valueOf(ret);
            byte[] retByteArr = Base64.decode(retString);
            ByteBufferReader br = new ByteBufferReader(retByteArr);
            int taskArrLen = br.readInt();
            for (int i = 0; i < taskArrLen; i++) {
                partList.add(new TowerPart(br));
            }
            return partList;
        } catch (SocketTimeoutException timeoutException) {
            XLog.i("XeroApiImpl queryTaskParts SocketTimeoutException" + timeoutException.getMessage());
            throw new RuntimeException(ERROR_NETWORK_TIMEOUT);
        } catch (Exception e) {
            XLog.i("XeroApiImpl queryTaskParts Exception" + e.getMessage());
            throw new RuntimeException(ERROR_NETWORK);
        }
    }

    public static void resetNetConnect() {
        IP = SettingHelper.getServerPort();
        URL = "http://" + SettingHelper.getServerPort() + "/TMSServ/TMSService.asmx";
        sessionId = 0;
        ht = null;
    }
}
