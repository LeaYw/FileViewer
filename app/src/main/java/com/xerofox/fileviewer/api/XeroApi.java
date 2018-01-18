package com.xerofox.fileviewer.api;

import android.arch.lifecycle.LiveData;

import com.xerofox.fileviewer.AppExecutors;
import com.xerofox.fileviewer.vo.Resource;
import com.xerofox.fileviewer.vo.Task;
import com.xerofox.fileviewer.vo.TowerPart;

import java.util.List;

public interface XeroApi {

    LiveData<ApiResponse<List<Task>>> loadAllTasks();

    LiveData<Resource<List<Task>>> getServerTasks(AppExecutors appExecutors);

    LiveData<ApiResponse<List<Task>>> getServerTasks(AppExecutors appExecutors, int[] localTaskIds);

    LiveData<Resource<List<Task>>> downloadTasks(AppExecutors appExecutors, FileHelper fileHelper, List<Task> data);

    LiveData<Resource<List<Integer>>> checkUpdate(AppExecutors appExecutors, int id, List<TowerPart> parts);

    LiveData<Resource<Boolean>> downloadTowerParts(AppExecutors appExecutors, FileHelper fileHelper, Task task, List<Integer> idArray);
}
