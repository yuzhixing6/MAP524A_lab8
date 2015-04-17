package seneca.ict.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class CoursesProvider extends ContentProvider
{
	static final String PROVIDER_NAME =
		"seneca.ict.provider";

	static final Uri CONTENT_URI =
		Uri.parse("content://"+ PROVIDER_NAME + "/courses");

	static final String _ID = "_id";
	static final String TITLE = "title";
	static final String CODE = "code";
    static final String ROOM = "room";

	static final int COURSES = 1;
	static final int COURSE_ID = 2;

	private static final UriMatcher uriMatcher;
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "courses", COURSES);
		uriMatcher.addURI(PROVIDER_NAME, "courses/#", COURSE_ID);
	}

	//---for database use---
	SQLiteDatabase coursesDB;
	static final String DATABASE_NAME = "Courses";
	static final String DATABASE_TABLE = "Titles";
	static final int DATABASE_VERSION = 1;
	static final String DATABASE_CREATE =
		"create table " + DATABASE_TABLE +
		" (_id integer primary key autoincrement, "
		+ "title text not null, code text not null, room text not null);";

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			Log.w("CP database",
					"Upgrading database from version " +
							oldVersion + " to " + newVersion +
					", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS titles");
			onCreate(db);
		}
	}
	
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// arg0 = uri 
		// arg1 = selection
		// arg2 = selectionArgs
		int count=0;
		switch (uriMatcher.match(arg0)){
		case COURSES:
			count = coursesDB.delete(
					DATABASE_TABLE,
					arg1,
					arg2);
			break;
		case COURSE_ID:
			String id = arg0.getPathSegments().get(1);
			count = coursesDB.delete(
					DATABASE_TABLE,
					_ID + " = " + id +
					(!TextUtils.isEmpty(arg1) ? " AND (" +
							arg1 + ')' : ""),
							arg2);
			break;
		default: throw new IllegalArgumentException("Unknown URI " + arg0);
		}
		getContext().getContentResolver().notifyChange(arg0, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)){
		//---get all courses---
		case COURSES:
			return "vnd.android.cursor.dir/vnd.senecacollege.courses ";
			
		//---get a particular book---
		case COURSE_ID:
			return "vnd.android.cursor.item/vnd.senecacollege.courses ";
			
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//---add a new book---
		long rowID = coursesDB.insert(
				DATABASE_TABLE,
				"",
				values);

		//---if added successfully---
		if (rowID>0)
		{
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		coursesDB = dbHelper.getWritableDatabase();
		return (coursesDB == null)? false:true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);

		if (uriMatcher.match(uri) == COURSE_ID)
			//---if getting a particular book---
			sqlBuilder.appendWhere(
					_ID + " = " + uri.getPathSegments().get(1));

		if (sortOrder==null || sortOrder=="")
			sortOrder = TITLE;

		Cursor c = sqlBuilder.query(
			coursesDB,
			projection,
			selection,
			selectionArgs,
			null,
			null,
			sortOrder);

		//---register to watch a content URI for changes---
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
		case COURSES:
			count = coursesDB.update(
					DATABASE_TABLE,
					values,
					selection,
					selectionArgs);
			break;
		case COURSE_ID:
			count = coursesDB.update(
					DATABASE_TABLE,
					values,
					_ID + " = " + uri.getPathSegments().get(1) +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		default: throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}