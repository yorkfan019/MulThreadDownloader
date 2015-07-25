package com.york.org.multhreaddownloader.util;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.york.org.multhreaddownloader.service.DBOpenHelper;

public class UploadLogService {
	private DBOpenHelper2 dbOpenHelper;
	
	public UploadLogService(Context context){
		this.dbOpenHelper = new DBOpenHelper2(context);
	}
	
	public void save(String sourceid, File uploadFile){
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		db.execSQL("insert into uploadlog(uploadfilepath, sourceid) values(?,?)",
				new Object[]{uploadFile.getAbsolutePath(),sourceid});
	}
	
	public void delete(File uploadFile){
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		db.execSQL("delete from uploadlog where uploadfilepath=?", new Object[]{uploadFile.getAbsolutePath()});
	}
	
	public String getBindId(File uploadFile){
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("select sourceid from uploadlog where uploadfilepath=?", 
				new String[]{uploadFile.getAbsolutePath()});
		if(cursor.moveToFirst()){
			return cursor.getString(0);
		}
		return null;
	}
}
