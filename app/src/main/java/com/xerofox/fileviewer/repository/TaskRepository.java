package com.xerofox.fileviewer.repository;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.elvishew.xlog.XLog;
import com.xerofox.fileviewer.AppExecutors;
import com.xerofox.fileviewer.api.ApiResponse;
import com.xerofox.fileviewer.api.FileHelper;
import com.xerofox.fileviewer.api.XeroApi;
import com.xerofox.fileviewer.vo.Resource;
import com.xerofox.fileviewer.vo.Task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskRepository {
    private final AppExecutors appExecutors;
    private final FileHelper fileHelper;
    private final XeroApi api;

    @Inject
    public TaskRepository(AppExecutors appExecutors, FileHelper fileHelper, XeroApi api) {
        this.appExecutors = appExecutors;
        this.fileHelper = fileHelper;
        this.api = api;
    }


    public LiveData<Resource<List<Task>>> getDownloadTasks() {
        return new NetworkBoundResource<List<Task>, List<Task>>(appExecutors) {

            @Override
            protected void saveCallResult(@NonNull List<Task> item) {
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Task> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Task>> loadFromDb() {
                return new LiveData<List<Task>>() {
                    @Override
                    protected void onActive() {
                        super.onActive();
                    }
                };
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Task>>> createCall() {
                return api.getServerTasks(appExecutors, fileHelper.getLocalTaskIds());
            }
        }.asLiveData();
//        return api.getServerTasks(appExecutors, fileHelper.getLocalTaskIds());
    }

    public LiveData<Resource<List<Task>>> loadAllTasks() {
        return new NetworkBoundResource<List<Task>, List<Task>>(appExecutors) {

            @Override
            protected void saveCallResult(@NonNull List<Task> item) {
                fileHelper.saveTasks(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Task> data) {
                return data == null || data.isEmpty();
            }

            @NonNull
            @Override
            protected LiveData<List<Task>> loadFromDb() {
                return fileHelper.loadAllTasks();
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Task>>> createCall() {
                return api.getServerTasks(appExecutors, fileHelper.getLocalTaskIds());
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<Task>>> loadActiveTasks() {
        return new NetworkBoundResource<List<Task>, List<Task>>(appExecutors) {

            @Override
            protected void saveCallResult(@NonNull List<Task> item) {
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Task> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Task>> loadFromDb() {
                return fileHelper.loadActiveTasks();
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Task>>> createCall() {
                return api.getServerTasks(appExecutors, fileHelper.getLocalTaskIds());
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<Task>>> loadLocalTasks() {
        return new NetworkBoundResource<List<Task>, List<Task>>(appExecutors) {

            @Override
            protected void saveCallResult(@NonNull List<Task> item) {
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Task> data) {
                return data == null || data.isEmpty();
            }

            @NonNull
            @Override
            protected LiveData<List<Task>> loadFromDb() {
                return fileHelper.loadActiveTasks();
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Task>>> createCall() {
                return api.loadAllTasks();
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<Task>>> loadServerTasks() {
        return api.getServerTasks(appExecutors);
    }


    public LiveData<Resource<List<Task>>> downloadTasks(List<Task> data) {
//        return new NetworkBoundResource<Boolean,Boolean>(appExecutors){
//
//            @Override
//            protected void saveCallResult(@NonNull Boolean item) {
//
//            }
//
//            @Override
//            protected boolean shouldFetch(@Nullable Boolean data) {
//                return true;
//            }
//
//            @NonNull
//            @Override
//            protected LiveData<Boolean> loadFromDb() {
//                return AbsentLiveData.create();
//            }
//
//            @NonNull
//            @Override
//            protected LiveData<ApiResponse<Boolean>> createCall() {
//                return null;
//            }
//        }.asLiveData();
        XLog.i("TaskRepository downloadTasks");
        return api.downloadTasks(appExecutors, fileHelper, data);
    }

    public void updateTask(Task task) {
        fileHelper.updateTask(task);
    }

    public boolean isTaskDownload(Task task) {
        int[] ids = fileHelper.getLocalTaskIds();
        for (int id : ids) {
            if (id == task.getId()) {
                return true;
            }
        }
        return false;
    }
}
