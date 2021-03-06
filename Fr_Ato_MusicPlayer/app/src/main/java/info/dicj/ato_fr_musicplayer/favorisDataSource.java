package info.dicj.ato_fr_musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import info.dicj.ato_fr_musicplayer.items.enregistrementFavoris;
import info.dicj.ato_fr_musicplayer.items.enregistrementTheme;

/**
 * Created by utilisateur on 09/03/2017.
 */
public class favorisDataSource
{
    private SQLiteDatabase database;
    private mySQLiteHelper dbHelper;

    private String[] allColumns = { mySQLiteHelper.COLUMN_ID,
            mySQLiteHelper.COLUMN_INDICEMUSIQUE };

    private String[] allColumnsTheme = { mySQLiteHelper.COLUMN_ID,
            mySQLiteHelper.COLUMN_NOMTHEME };

    public favorisDataSource(Context context)
    {
        dbHelper = new mySQLiteHelper(context);
    }

    public void open() throws SQLException
    {
        database = dbHelper.getWritableDatabase();
    }

    public void close()
    {
        dbHelper.close();
    }

    public enregistrementFavoris createEnregistrementFavoris(int indiceMusique)
    {

        ContentValues values = new ContentValues();
        values.put(mySQLiteHelper.COLUMN_INDICEMUSIQUE, indiceMusique);
        long insertId = database.insert(mySQLiteHelper.TABLE_FAVORIS, null, values);
        Cursor cursor = database.query(mySQLiteHelper.TABLE_FAVORIS,
                allColumns, mySQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        enregistrementFavoris newEnregistrementFavoris = cursorToEnregistrementFavoris(cursor);
        cursor.close();

        return newEnregistrementFavoris;
    }

    public enregistrementTheme updateTheme(String nomTheme)
    {

        ContentValues values = new ContentValues();
        values.put(mySQLiteHelper.COLUMN_NOMTHEME, nomTheme);
        database.update(mySQLiteHelper.TABLE_THEME, values, "id=" + 1, null);

        return null;
    }

    public enregistrementTheme getTheme()
    {
        enregistrementTheme theme = new enregistrementTheme();

        Cursor cursor = database.query(mySQLiteHelper.TABLE_THEME,
                allColumnsTheme, null, null, null, null, null);

        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
           theme = cursorEnregistrementTheme(cursor);
           cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();

        return theme;
    }



    public List<enregistrementFavoris> getAllEnregistrements() {

        List<enregistrementFavoris> listeEnregistrementFavoris = new ArrayList<enregistrementFavoris>();

        Cursor cursor = database.query(mySQLiteHelper.TABLE_FAVORIS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            enregistrementFavoris enregistrementFavoris = cursorToEnregistrementFavoris(cursor);
            listeEnregistrementFavoris.add(enregistrementFavoris);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return listeEnregistrementFavoris;
    }

    public void deleteEnregistrementFavoris(enregistrementFavoris enregistrementFavoris) {

        long id = enregistrementFavoris.getId();
        System.out.println("Comment deleted with id: " + id);
        database.delete(mySQLiteHelper.TABLE_FAVORIS, mySQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void deleteEnregistrementFavoris(int indiceMusique) {

        //long id = enregistrementFavoris.getId();
        System.out.println("Comment deleted with indiceMusique: " + indiceMusique);
        database.delete(mySQLiteHelper.TABLE_FAVORIS, mySQLiteHelper.COLUMN_INDICEMUSIQUE
                + " = " + indiceMusique, null);
    }

    /*public void deleteEnregistrementFavoris(int position) //id de la musique
    {

        //long id = enregistrementFavoris.getId();
        System.out.println("Comment deleted with position: " + position);
        database.delete(mySQLiteHelper.TABLE_FAVORIS, mySQLiteHelper.COLUMN_ID
                + " = " + position, null);
    }*/

    private enregistrementFavoris cursorToEnregistrementFavoris(Cursor cursor) {

        enregistrementFavoris enregistrementFavoris = new enregistrementFavoris();
        enregistrementFavoris.setId(cursor.getLong(0));
        enregistrementFavoris.setIndiceMusique(cursor.getInt(1));
        return enregistrementFavoris;
    }

    private enregistrementTheme cursorEnregistrementTheme(Cursor cursor)
    {
        enregistrementTheme theme = new enregistrementTheme();
        theme.setId(cursor.getLong(0));
        theme.setNomTheme(cursor.getString(1));
        return  theme;
    }
}
