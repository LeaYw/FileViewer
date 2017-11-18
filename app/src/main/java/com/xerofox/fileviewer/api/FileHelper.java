package com.xerofox.fileviewer.api;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.xerofox.fileviewer.vo.Task;
import com.xerofox.fileviewer.vo.TowerPart;

import java.util.ArrayList;
import java.util.List;

public interface FileHelper {
    String PART_FOLDER_SUFFIX = "Files";

    @NonNull
    LiveData<List<Task>> loadAllTasks();

    void saveTasks(List<Task> tasks);

    LiveData<ArrayList<TowerPart>> loadTowerParts(Task task);
}