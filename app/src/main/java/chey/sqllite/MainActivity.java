package chey.sqllite;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import chey.sqllite.sqlite.FeedReaderContract;
import chey.sqllite.sqlite.FeedReaderDbHelper;

public class MainActivity extends AppCompatActivity {
    FeedReaderDbHelper mDbHelper;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDbHelper = new FeedReaderDbHelper(this);
        listView = (ListView) findViewById(R.id.listView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

    public void insert(final View view){
        LayoutInflater li = LayoutInflater.from(this);
        View dialog_view = li.inflate(R.layout.dialog_create, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(dialog_view);

        final EditText editText = (EditText) dialog_view.findViewById(R.id.editText);
        final EditText editText2 = (EditText) dialog_view.findViewById(R.id.editText2);

        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String title = editText.getText().toString();
                String subtitle = editText2.getText().toString();

                if(title.equals("") || (subtitle.equals("")) ){
                    show_snackbar(view, "Please fill in the blanks");
                    return;
                }

                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, title);
                values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, subtitle);

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);

                if(newRowId == -1){
                    show_snackbar(view, "Create failed");
                }else{
                    show_snackbar(view, "Create passed");
                }
            }
        });

        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void read(final View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Filter");
        alert.setMessage("Search by title or show all?");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Search by title", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();

                if(title.equals("")){
                    show_snackbar(view, "Please fill in the blank");
                }else{
                    read_filter(view, title);
                }
            }
        });

        alert.setNegativeButton("Show all",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        read_filter(view, null);
                    }
                });
        alert.show();
        /*


        textView_title.setText(itemIds.toString());*/
    }

    public void read_filter(final View view, String title){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE
        };

        // Filter results WHERE "title" = ?
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " = ?";
        String[] selectionArgs = { title };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

        Cursor cursor;
        if(title != null) {
            cursor = db.query(
                    FeedReaderContract.FeedEntry.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        }else{
            cursor = db.query(
                    FeedReaderContract.FeedEntry.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        }

        List values = new ArrayList<>();
        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry._ID));
            String title2 = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE));
            String subtitle2 = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE));

            values.add(Long.toString(itemId) + "," + title2 + "," + subtitle2);
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, android.R.id.text1, values);

        // Assign adapter to ListView
        listView.setAdapter(adapter);

        if(values.isEmpty()){
            show_snackbar(view, "List empty.");
        }
    }


    public void delete(final View view){
        int pos = listView.getCheckedItemPosition();

        if(pos == -1){
            show_snackbar(view, "Please select an item to delete");
            return;
        }

        String s = (String) listView.getAdapter().getItem(pos);
        String[] strings = s.split(",");
        String id = strings[0];

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Define 'where' part of query.
        String selection = FeedReaderContract.FeedEntry._ID + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { id };

        // Issue SQL statement.
        db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);

        read_filter(view, null);
    }

    public void update(final View view){
        int pos = listView.getCheckedItemPosition();

        if(pos == -1){
            show_snackbar(view, "Please select an item to update");
            return;
        }

        String s = (String) listView.getAdapter().getItem(pos);
        String[] strings = s.split(",");
        final String id = strings[0];

        LayoutInflater li = LayoutInflater.from(this);
        View dialog_view = li.inflate(R.layout.dialog_create, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(dialog_view);

        final EditText editText = (EditText) dialog_view.findViewById(R.id.editText);
        final EditText editText2 = (EditText) dialog_view.findViewById(R.id.editText2);

        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String title = editText.getText().toString();
                String subtitle = editText2.getText().toString();

                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                // New value for one column
                ContentValues values = new ContentValues();
                values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, title);
                values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, subtitle);

                // Which row to update, based on the id
                String selection = FeedReaderContract.FeedEntry._ID + " LIKE ?";
                String[] selectionArgs = { id };

                int count = db.update(
                        FeedReaderContract.FeedEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                if(count == 0){
                    show_snackbar(view, "Update failed");
                }else{
                    show_snackbar(view, "Update passed");
                }

                read_filter(view, null);
            }
        });

        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


    }

    public void show_snackbar(View view, String s){
        final Snackbar snackBar = Snackbar.make(view, s, Snackbar.LENGTH_SHORT);

        snackBar.setAction("Ok", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackBar.dismiss();
            }
        });
        snackBar.show();
    }
}
