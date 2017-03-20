package de.xikolo.storages.databases.adapters;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

import de.xikolo.models.Section;
import de.xikolo.storages.databases.DatabaseHelper;
import de.xikolo.storages.databases.tables.ModuleTable;
import de.xikolo.storages.databases.tables.Table;

public class ModuleDataAdapter extends DataAdapter<Section> {

    private ProgressDataAdapter progressDataAccess;

    public ModuleDataAdapter(DatabaseHelper databaseHelper, Table table, Table progressTable) {
        super(databaseHelper, table);

        this.progressDataAccess = new ProgressDataAdapter(databaseHelper, progressTable);
    }

    @Override
    protected Section buildEntity(Cursor cursor) {
        Section module = new Section();

        module.id = cursor.getString(0);
        module.position = cursor.getInt(1);
        module.name = cursor.getString(2);
        module.available_from = cursor.getString(3);
        module.available_to = cursor.getString(4);
        module.locked = cursor.getInt(5) != 0;
        module.courseId = cursor.getString(6);

        return module;
    }

    @Override
    protected ContentValues buildContentValues(Section module) {
        ContentValues values = new ContentValues();

        values.put(ModuleTable.COLUMN_ID, module.id);
        values.put(ModuleTable.COLUMN_POSITION, module.position);
        values.put(ModuleTable.COLUMN_NAME, module.name);
        values.put(ModuleTable.COLUMN_AVAILABLE_FROM, module.available_from);
        values.put(ModuleTable.COLUMN_AVAILABLE_TO, module.available_to);
        values.put(ModuleTable.COLUMN_LOCKED, module.locked);
        values.put(ModuleTable.COLUMN_COURSE_ID, module.courseId);

        return values;
    }

    public void add(Section module, boolean includeProgress) {
        super.add(module);

        if (includeProgress) {
            progressDataAccess.addOrUpdate(module.progress);
        }
    }

    public void addOrUpdate(Section module, boolean includeProgress) {
        if (update(module, includeProgress) < 1) {
            add(module, includeProgress);
        }
    }

    @Override
    public Section get(String id) {
        Section module = super.get(id);
        module.progress = progressDataAccess.get(id);
        return module;
    }

    @Override
    public List<Section> getAll() {
        List<Section> courseList = super.getAll();

        for (Section module : courseList) {
            module.progress = progressDataAccess.get(module.id);
        }

        return courseList;
    }

    public List<Section> getAllForCourse(String courseId) {
        List<Section> moduleList = super.getAll("SELECT * FROM " + ModuleTable.TABLE_NAME + " WHERE " + ModuleTable.COLUMN_COURSE_ID + " = \'" + courseId + "\'");

        for (Section module : moduleList) {
            module.progress = progressDataAccess.get(module.id);
        }

        return moduleList;
    }

    public int update(Section module, boolean includeProgress) {
        int affected =  super.update(module);

        if (includeProgress) {
            progressDataAccess.addOrUpdate(module.progress);
        }

        return affected;
    }

    @Override
    public void delete(String id) {
        super.delete(id);

        progressDataAccess.delete(id);
    }

}
