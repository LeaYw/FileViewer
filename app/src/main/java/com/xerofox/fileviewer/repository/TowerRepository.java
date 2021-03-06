package com.xerofox.fileviewer.repository;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.xerofox.fileviewer.AppExecutors;
import com.xerofox.fileviewer.api.ApiResponse;
import com.xerofox.fileviewer.api.FileHelper;
import com.xerofox.fileviewer.api.XeroApi;
import com.xerofox.fileviewer.vo.ManuMenuFilter;
import com.xerofox.fileviewer.vo.MaterialMenuFilter;
import com.xerofox.fileviewer.vo.MenuFilter;
import com.xerofox.fileviewer.vo.Resource;
import com.xerofox.fileviewer.vo.SpecificationMenuFilter;
import com.xerofox.fileviewer.vo.Task;
import com.xerofox.fileviewer.vo.TowerPart;
import com.xerofox.fileviewer.vo.TowerTypeFilter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TowerRepository {

    private final AppExecutors appExecutors;
    private final FileHelper fileHelper;
    private final XeroApi api;

    @Inject
    TowerRepository(AppExecutors appExecutors, FileHelper fileHelper, XeroApi api) {
        this.appExecutors = appExecutors;
        this.fileHelper = fileHelper;
        this.api = api;
    }

    public LiveData<ArrayList<TowerPart>> getTowerParts(Task task, SparseArray<MenuFilter> filters) {
        return fileHelper.loadTowerParts(task, filters);
    }

    public LiveData<List<String>> getFilterTitles() {
        return new LiveData<List<String>>() {
            @Override
            protected void onActive() {
                super.onActive();
                List<String> list = new ArrayList<>();
                list.add("件号");
                list.add("规格");
                list.add("工艺");
                list.add("材质");
                list.add("数量");
                list.add("塔型");
                list.add("备注");

                setValue(list);
            }
        };
    }

    public LiveData<List<List<MenuFilter>>> getFilterLists(Task task) {
        return new LiveData<List<List<MenuFilter>>>() {
            @Override
            protected void onActive() {
                super.onActive();
                List<List<MenuFilter>> lists = new ArrayList<>();

                //件号
                lists.add(new ArrayList<>());

                //规格filter
                List<MenuFilter> menuFilters1 = new ArrayList<>();
                menuFilters1.add(new SpecificationMenuFilter(SpecificationMenuFilter.MIN, 56));
                menuFilters1.add(new SpecificationMenuFilter(63, 125));
                menuFilters1.add(new SpecificationMenuFilter(140, SpecificationMenuFilter.MAX));
                menuFilters1.add(new SpecificationMenuFilter(SpecificationMenuFilter.MIN, SpecificationMenuFilter.MAX));
                lists.add(menuFilters1);

                //工艺filter
                List<MenuFilter> menuFilters2 = new ArrayList<>();
                menuFilters2.add(new ManuMenuFilter(TowerPart.MANU_ZHIWAN) {
                    @Override
                    public boolean match(TowerPart part) {
                        return part.getManuHourZhiWan() > 0;
                    }
                });
                menuFilters2.add(new ManuMenuFilter(TowerPart.MANU_CUT_ANGEL) {
                    @Override
                    public boolean match(TowerPart part) {
                        return part.getManuHourCutAngle() > 0;
                    }
                });
                menuFilters2.add(new ManuMenuFilter(TowerPart.MANU_KAIHE) {
                    @Override
                    public boolean match(TowerPart part) {
                        return part.getManuHourKaiHe() > 0;
                    }
                });
                menuFilters2.add(new ManuMenuFilter(MenuFilter.CLEAR) {

                    @Override
                    public boolean match(TowerPart part) {
                        return true;
                    }
                });
                lists.add(menuFilters2);

                //材质
                List<MenuFilter> menuFilters3 = new ArrayList<>();
                menuFilters2.add(new MaterialMenuFilter("Q235"));
                menuFilters2.add(new MaterialMenuFilter("Q345"));
                menuFilters2.add(new MaterialMenuFilter("Q390"));
                menuFilters2.add(new MaterialMenuFilter("Q420"));
                menuFilters2.add(new MaterialMenuFilter("Q490"));
                menuFilters2.add(new ManuMenuFilter(MenuFilter.CLEAR) {

                    @Override
                    public boolean match(TowerPart part) {
                        return true;
                    }
                });
                lists.add(menuFilters3);

                //数量
                lists.add(new ArrayList<>());

                //塔型filter
                Task t = fileHelper.loadTask(task);
                if (t != null && t.getPartList() != null && !t.getPartList().isEmpty()) {
                    List<MenuFilter> menuFilters = new ArrayList<>();
                    for (TowerPart part : t.getPartList()) {
                        TowerTypeFilter filter = new TowerTypeFilter(part.getTowerTypeName());
                        if (!menuFilters.contains(filter)) {
                            menuFilters.add(filter);
                        }
                    }
                    lists.add(menuFilters);
                }

                //备注
                lists.add(new ArrayList<>());

                setValue(lists);
            }
        };
    }

    public LiveData<Resource<List<Integer>>> checkUpdate(int id, List<TowerPart> parts) {
        return api.checkUpdate(appExecutors, id, parts);
    }

    public LiveData<Resource<Boolean>> downloadTowerPart(Task task, List<Integer> idArray) {
        return api.downloadTowerParts(appExecutors, fileHelper, task, idArray);
    }
}
