package com.xerofox.fileviewer.ui.task;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingComponent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.xerofox.fileviewer.R;
import com.xerofox.fileviewer.databinding.SearchFragmentBinding;
import com.xerofox.fileviewer.helper.SettingHelper;
import com.xerofox.fileviewer.ui.common.BaseFragment;
import com.xerofox.fileviewer.ui.index.NavigationController;
import com.xerofox.fileviewer.ui.part.TowerPartActivity;
import com.xerofox.fileviewer.util.AutoClearedValue;
import com.xerofox.fileviewer.util.ToastUtils;
import com.xerofox.fileviewer.vo.Task;

import java.util.List;

import javax.inject.Inject;

public class TaskFragment extends BaseFragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    DataBindingComponent dataBindingComponent;

    @Inject
    NavigationController navigationController;

    AutoClearedValue<SearchFragmentBinding> binding;

    AutoClearedValue<TaskListAdapter> adapter;

    private TaskViewModel taskViewModel;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_task, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                navigationController.navigateToSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        SearchFragmentBinding dataBinding = DataBindingUtil
                .inflate(inflater, R.layout.search_fragment, container, false,
                        dataBindingComponent);
        binding = new AutoClearedValue<>(this, dataBinding);
        return dataBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.task_list);
//        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        taskViewModel = ViewModelProviders.of(this, viewModelFactory).get(TaskViewModel.class);
        initRecyclerView();
        TaskListAdapter rvAdapter = new TaskListAdapter(this::onItemClick);
        binding.get().list.setAdapter(rvAdapter);
        adapter = new AutoClearedValue<>(this, rvAdapter);
        taskViewModel.setIsLocal(!SettingHelper.isAutoQuery());
        setHasOptionsMenu(true);
    }

    private void onItemClick(Task task) {
        if (!SettingHelper.isAutoQuery()) {
            Intent intent = new Intent(getActivity(), TowerPartActivity.class);
            intent.putExtra(TowerPartActivity.ARG_TASK, task);
            startActivity(intent);
        } else {
            taskViewModel.download(task);
        }
    }

    private void initRecyclerView() {
        taskViewModel.getTasks().observe(this, result -> {
            binding.get().setResultCount(result == null || result.data == null ? 0 : result.data.size());
            adapter.get().replace(result == null ? null : result.data);
            binding.get().executePendingBindings();
        });

        taskViewModel.getDownloadState().observe(this, downloadState -> {
            if (downloadState != null) {
                if (downloadState.isDownloading()) {
                    showProgressDialog();
                } else {
                    dismissProgressDialog();
                    List<Task> data = downloadState.getData();
                    if (data != null && !data.isEmpty()) {
                        ToastUtils.showToast(R.string.download_success);
                        Intent intent = new Intent(getActivity(), TowerPartActivity.class);
                        intent.putExtra(TowerPartActivity.ARG_TASK, data.get(0));
                        startActivity(intent);
                    } else {
                        ToastUtils.showToast(downloadState.getErrorMessage());
                    }
                }
            }
        });

    }
}
