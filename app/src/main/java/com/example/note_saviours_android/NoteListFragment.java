package com.example.note_saviours_android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class NoteListFragment extends ListFragment {
    private ArrayList<Note> mNotes;
    private boolean mSubtitleVisible;
    private static final int PERMISSION_REQUESTS = 1;
    private static final String TAG = "NoteListFragment";
    private NoteAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mSubtitleVisible = false;

        getActivity().setTitle(R.string.notes_title);
        mNotes = Notebook.getInstance(getActivity()).getNotes();

        adapter = new NoteAdapter(mNotes);
        setListAdapter(adapter);
    }

    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup parent,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, parent, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (mSubtitleVisible) {
                getActivity().getActionBar().setSubtitle(R.string.subtitle);
            }
        }
        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
        // getListView() will also retrieve the view, but returns null
        // until after onCreateView returns
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Use floating context menu on Froyo and Gingerbread
            registerForContextMenu(listView);
        } else {
            // Use contextual action bar on Honeycomb and higher
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            listView.setMultiChoiceModeListener(
                    new AbsListView.MultiChoiceModeListener() {
                        @Override
                        public void onItemCheckedStateChanged(ActionMode mode,
                                                              int position,
                                                              long id,
                                                              boolean checked) {
                            // This space intentionally left blank
                        }

                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            // Called when ActionMode is created to inflate the menu
                            // Use the inflater from the action mode rather than the
                            // activity, because it has details for configuring
                            // the contextual action bar
                            MenuInflater inflater = mode.getMenuInflater();
                            inflater.inflate(R.menu.note_list_item_context, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            // Called after onCreateActionMode and whenever an existing
                            // contextual action bar needs to be refreshed with new data
                            // This space intentionally left blank
                            return false;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode,
                                                           MenuItem item) {
                            // Called when the user selects an action
                            boolean selectionHandled;

                            switch (item.getItemId()) {
                                case R.id.menu_item_delete_note:
                                    NoteAdapter adapter =
                                            (NoteAdapter) getListAdapter();
                                    Notebook notebook =
                                            Notebook.getInstance(getActivity());

                                    for (int i = adapter.getCount() - 1; i >= 0; i--) {
                                        if (getListView().isItemChecked(i)) {
                                            notebook.deleteNote(adapter.getItem(i));
                                        }
                                    }

                                    Toast
                                            .makeText(getActivity(),
                                                    getResources()
                                                            .getString(R.string.notes_deleted),
                                                    Toast.LENGTH_LONG).show();

                                    // Prepare the action mode to be destroyed
                                    mode.finish();
                                    adapter.notifyDataSetChanged();
                                    selectionHandled = true;
                                    break;
                                default:
                                    selectionHandled = false;
                                    break;
                            }

                            return selectionHandled;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            // Called when ActionMode is about the be destroyed from
                            // the user cancelling or the action being responded to
                            // This space intentionally left blank
                        }
                    });
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_note_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,
                                    View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.note_list_item_context,
                menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean selectionHandled;

        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        NoteAdapter adapter = (NoteAdapter) getListAdapter();
        Note note = adapter.getItem(position);

        switch (item.getItemId()) {
            case R.id.menu_item_delete_note:
                Notebook.getInstance(getActivity()).deleteNote(note);
                adapter.notifyDataSetChanged();
                selectionHandled = true;
                break;
            default:
                selectionHandled = super.onContextItemSelected(item);
                break;
        }

        return selectionHandled;
    }

    @TargetApi(11)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.menu_item_new_note:
                final Note note = new Note();
                Notebook.getInstance(getActivity()).addNote(note);

                Intent intent =
                        new Intent(getActivity(), NotePagerActivity.class);
                intent.putExtra(NoteFragment.EXTRA_NOTE_ID, note.getId());
                startActivityForResult(intent, 0);

                selectionHandled = true;
                break;
            case R.id.menu_item_category_sort:
                Collections.sort(mNotes, new Comparator<Note>() {
                    @Override
                    public int compare(Note o1, Note o2) {
                        return o1.getCategory().compareTo(o2.getCategory());
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case R.id.menu_item_datetime_sort:
                Collections.sort(mNotes, new Comparator<Note>() {
                    @Override
                    public int compare(Note o1, Note o2) {
                        return o2.getDate().compareTo(o1.getDate());
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            default:
                selectionHandled = super.onOptionsItemSelected(item);
                break;
        }

        return selectionHandled;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = ((NoteAdapter) getListAdapter()).getItem(position);

        // Start NotePagerActivity with this note
        // NoteListFragment uses getActivity() to pass its hosting
        // activity as the Context object that the Intent constructor needs
        Intent intent = new Intent(getActivity(), NotePagerActivity.class);
        intent.putExtra(NoteFragment.EXTRA_NOTE_ID, note.getId());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        // Update the list view onResume,
        // as it might have been paused not killed
        super.onResume();
        ((NoteAdapter) getListAdapter()).notifyDataSetChanged();
    }

    private class NoteAdapter extends ArrayAdapter<Note> {
        public NoteAdapter(ArrayList<Note> notes) {
            // Required to properly hook up dataset of Notes
            // Not using a pre-defined layout, so pass 0 for the layout ID
            super(getActivity(), 0, notes);
        }

        private String getFormattedDate(FragmentActivity activity,
                                        Note note) {
            // TODO: Duplicated code, original in NoteFragment.java
            String formattedDate = "";

            if (activity != null) {
                Date date = note.getDate();
                DateFormat dateFormat = android.text.format.DateFormat
                        .getDateFormat(activity.getApplicationContext());
                DateFormat timeFormat = android.text.format.DateFormat
                        .getTimeFormat(activity.getApplicationContext());
                formattedDate = dateFormat.format(date) +
                        " " +
                        timeFormat.format(date);
            }

            return formattedDate;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If we weren't give a view, inflate one
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.list_item_note, null);
            }

            // Configure the view for this Note
            Note note = getItem(position);

            TextView titleTextView = (TextView) convertView
                    .findViewById(R.id.note_list_item_titleTextView);
            titleTextView.setText(note.getTitle());

            TextView dateTextView = (TextView) convertView
                    .findViewById(R.id.note_list_item_dateTextView);
            dateTextView.setText(getFormattedDate(getActivity(), note));

            CheckBox completeCheckBox = (CheckBox) convertView
                    .findViewById(R.id.note_list_item_completeCheckBox);
            completeCheckBox.setChecked(note.isComplete());

            return convertView;
        }
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(getActivity(), permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    getActivity(), allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(getActivity(), permission)) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    getActivity().getPackageManager()
                            .getPackageInfo(getActivity().getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

}

