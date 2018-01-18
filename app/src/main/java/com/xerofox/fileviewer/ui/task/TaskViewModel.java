package com.xerofox.fileviewer.ui.task;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.elvishew.xlog.XLog;
import com.xerofox.fileviewer.repository.TaskRepository;
import com.xerofox.fileviewer.ui.common.DownloadState;
import com.xerofox.fileviewer.vo.Resource;
import com.xerofox.fileviewer.vo.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TaskViewModel extends ViewModel {

    private final LiveData<Resource<List<Task>>> tasks;
    private final MutableLiveData<Boolean> isLocal = new MutableLiveData<>();
    private final UpdateTaskHandler updateTaskHandler;
    private final DownloadTaskHandler downloadTaskHandler;
    private final CheckDownloadHandler checkDownloadHandler;

    @Inject
    TaskViewModel(TaskRepository repository) {
        updateTaskHandler = new UpdateTaskHandler(repository);
        downloadTaskHandler = new DownloadTaskHandler(repository);
        checkDownloadHandler = new CheckDownloadHandler(repository);
        tasks = Transformations.switchMap(isLocal, data -> {
            if (!data) {
                return repository.loadServerTasks();
            } else {
                return repository.loadLocalTasks();
            }
        });
    }

    void setIsLocal(boolean isLocal) {
        if (this.isLocal.getValue() != null && this.isLocal.getValue() == isLocal) {
            return;
        }
        this.isLocal.setValue(isLocal);
    }

    LiveData<Resource<List<Task>>> getTasks() {
        return tasks;
    }

    void updateTaskState(Task task) {
        updateTaskHandler.update(task);
    }

    public boolean isTaskDownload(Task task) {
        return checkDownloadHandler.isTaskDownload(task);
    }

    static class CheckDownloadHandler {
        private final TaskRepository repository;

        public CheckDownloadHandler(TaskRepository repository) {
            this.repository = repository;
        }

        private boolean isTaskDownload(Task task) {
            return repository.isTaskDownload(task);
        }
    }

    static class UpdateTaskHandler {
        private final TaskRepository repository;

        public UpdateTaskHandler(TaskRepository repository) {
            this.repository = repository;
        }

        void update(Task task) {
            repository.updateTask(task);
        }

    }

    LiveData<DownloadState<Task>> getDownloadState() {
        return downloadTaskHandler.getDownloadState();
    }

    void download(Task task) {
        downloadTaskHandler.download(task);
    }

    void download(List<Task> tasks) {
        XLog.i("DownloadViewModel download");
        downloadTaskHandler.download(tasks);
    }

    static class DownloadTaskHandler implements Observer<Resource<List<Task>>> {
        private final TaskRepository repository;
        private LiveData<Resource<List<Task>>> downloadLiveData;
        private final MutableLiveData<DownloadState<Task>> downloadState = new MutableLiveData<>();

        @VisibleForTesting
        DownloadTaskHandler(TaskRepository repository) {
            this.repository = repository;
            reset();
        }

        void download(Task task) {
            if (task == null) {
                return;
            }
            List<Task> tasks = new ArrayList<>(1);
            tasks.add(task);
            download(tasks);
        }

        void download(List<Task> tasks) {
            if (tasks == null || tasks.isEmpty()) {
                return;
            }
            unregister();
            XLog.i("DownloadHandler download");
            downloadLiveData = repository.downloadTasks(tasks);
            downloadState.setValue(new DownloadState(true, null));
            downloadLiveData.observeForever(this);
        }

        @Override
        public void onChanged(@Nullable Resource<List<Task>> result) {
            if (result == null) {
                reset();
            } else {
                switch (result.status) {
                    case SUCCESS:
                        unregister();
                        DownloadState<Task> value = new DownloadState<>(false, null);
                        value.setData(result.data);
                        downloadState.setValue(value);
                        break;
                    case ERROR:
                        unregister();
                        DownloadState<Task> error = new DownloadState<>(false, result.message);
                        downloadState.setValue(error);
                        break;
                }
            }
        }

        private void unregister() {
            if (downloadLiveData != null) {
                downloadLiveData.removeObserver(this);
                downloadLiveData = null;
            }
        }

        private void reset() {
            unregister();
            DownloadState<Task> value = new DownloadState<>(false, null);
            downloadState.setValue(value);
        }

        public LiveData<DownloadState<Task>> getDownloadState() {
            return downloadState;
        }
    }
}
