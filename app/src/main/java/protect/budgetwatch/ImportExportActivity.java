package protect.budgetwatch;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportExportActivity extends AppCompatActivity
{
    private static final String TAG = "BudgetWatch";

    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;
    private static final int CHOOSE_EXPORT_FILE = 2;

    private ImportExportTask importExporter;
    private Map<String, DataFormat> _fileFormatMap;

    private final File sdcardDir = Environment.getExternalStorageDirectory();
    private final String exportFilename = "BudgetWatch";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_export_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        _fileFormatMap = ImmutableMap.<String, DataFormat>builder()
                .put(getResources().getString(R.string.csv), DataFormat.CSV)
                .put(getResources().getString(R.string.json), DataFormat.JSON)
                .put(getResources().getString(R.string.zip), DataFormat.ZIP)
                .build();

        for(int id : new int[]{R.id.importFileFormatSpinner, R.id.exportFileFormatSpinner})
        {
            final Spinner fileFormatSpinner = (Spinner) findViewById(id);
            List<String> names = new ArrayList<>(_fileFormatMap.keySet());
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, names);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fileFormatSpinner.setAdapter(dataAdapter);
        }

        // If the application does not have permissions to external
        // storage, ask for it now

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            if (ContextCompat.checkSelfPermission(ImportExportActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(ImportExportActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(ImportExportActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_EXTERNAL_STORAGE);
            }
        }

        Button exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startExport(getSelectedFormat(R.id.exportFileFormatSpinner));
            }
        });


        // Check that there is an activity that can bring up a file chooser
        final Intent intentPickAction = new Intent(Intent.ACTION_PICK);

        Button importFilesystem = (Button) findViewById(R.id.importOptionFilesystemButton);
        importFilesystem.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chooseFileWithIntent(intentPickAction);
            }
        });

        if(isCallable(getApplicationContext(), intentPickAction) == false)
        {
            findViewById(R.id.dividerImportFilesystem).setVisibility(View.GONE);
            findViewById(R.id.importOptionFilesystemTitle).setVisibility(View.GONE);
            findViewById(R.id.importOptionFilesystemExplanation).setVisibility(View.GONE);
            importFilesystem.setVisibility(View.GONE);
        }


        // Check that there is an application that can find content
        final Intent intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContentAction.setType("*/*");

        Button importApplication = (Button) findViewById(R.id.importOptionApplicationButton);
        importApplication.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chooseFileWithIntent(intentGetContentAction);
            }
        });

        if(isCallable(getApplicationContext(), intentGetContentAction) == false)
        {
            findViewById(R.id.dividerImportApplication).setVisibility(View.GONE);
            findViewById(R.id.importOptionApplicationTitle).setVisibility(View.GONE);
            findViewById(R.id.importOptionApplicationExplanation).setVisibility(View.GONE);
            importApplication.setVisibility(View.GONE);
        }


        // This option, to import from the fixed location, should always be present

        Button importButton = (Button)findViewById(R.id.importOptionFixedButton);
        importButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DataFormat format = getSelectedFormat(R.id.importFileFormatSpinner);
                File importFile = new File(sdcardDir, exportFilename + "." + format.extension());

                Uri uri = Uri.fromFile(importFile);
                try
                {
                    FileInputStream stream = new FileInputStream(importFile);
                    Log.d(TAG, "Starting import from fixed location: " + importFile.getAbsolutePath());
                    startImport(format, stream, uri);
                }
                catch(FileNotFoundException e)
                {
                    Log.e(TAG, "Could not import file " + importFile.getAbsolutePath(), e);
                    onImportComplete(false, uri);
                }
            }
        });
    }

    private DataFormat getSelectedFormat(int id)
    {
        final Spinner fileFormatSpinner = (Spinner) findViewById(id);
        String name = (String)fileFormatSpinner.getSelectedItem();
        DataFormat format = _fileFormatMap.get(name);
        return format;
    }

    private void startImport(DataFormat format, final InputStream target, final Uri targetUri)
    {
        ImportExportTask.TaskCompleteListener listener = new ImportExportTask.TaskCompleteListener()
        {
            @Override
            public void onTaskComplete(boolean success)
            {
                onImportComplete(success, targetUri);
            }
        };

        String filename = fileNameFromUri(targetUri);
        if(filename == null)
        {
            filename = targetUri.getPath();
        }

        if(format == null && filename != null)
        {
            // Attempt to guess the data format based on the extension
            Log.d(TAG, "Attempting to determine file type for: " + filename);

            for(Map.Entry<String, DataFormat> item : _fileFormatMap.entrySet())
            {
                String key = item.getKey();
                if(filename.toLowerCase().endsWith(key.toLowerCase()))
                {
                    format = item.getValue();
                    break;
                }
            }
        }

        if(format != null)
        {
            Log.d(TAG, "Starting import of file: " + filename);
            importExporter = new ImportExportTask(ImportExportActivity.this,
                    format, target, listener);
            importExporter.execute();
        }
        else
        {
            // If format is still null, then we do not know what to import
            Log.w(TAG, "Could not import " + filename + ", could not determine extension");
            onImportComplete(false, targetUri);

            try
            {
                target.close();
            }
            catch (IOException e)
            {
                Log.w(TAG, "Failed to close stream during import", e);
            }
        }
    }

    private void startExport(final DataFormat format)
    {
        final File exportFile = new File(sdcardDir, exportFilename + "." + format.extension());

        ImportExportTask.TaskCompleteListener listener = new ImportExportTask.TaskCompleteListener()
        {
            @Override
            public void onTaskComplete(boolean success)
            {
                onExportComplete(success, exportFile, format);
            }
        };

        importExporter = new ImportExportTask(ImportExportActivity.this,
                format, exportFile, listener);
        importExporter.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        if(requestCode == PERMISSIONS_EXTERNAL_STORAGE)
        {
            // If request is cancelled, the result arrays are empty.
            boolean success = grantResults.length > 0;

            for(int grant : grantResults)
            {
                if(grant != PackageManager.PERMISSION_GRANTED)
                {
                    success = false;
                }
            }

            if(success == false)
            {
                // External storage permission rejected, inform user that
                // import/export is prevented
                Toast.makeText(getApplicationContext(), R.string.noExternalStoragePermissionError,
                        Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onDestroy()
    {
        if(importExporter != null && importExporter.getStatus() != AsyncTask.Status.RUNNING)
        {
            importExporter.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String fileNameFromUri(Uri uri)
    {
        if("file".equals(uri.getScheme()))
        {
            return uri.getPath();
        }

        Cursor returnCursor =
                getContentResolver().query(uri, null, null, null, null);
        if(returnCursor == null)
        {
            return null;
        }

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if(returnCursor.moveToFirst() == false)
        {
            returnCursor.close();
            return null;
        }

        String name = returnCursor.getString(nameIndex);
        returnCursor.close();

        return name;
    }

    private void onImportComplete(boolean success, Uri path)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(success)
        {
            builder.setTitle(R.string.importSuccessfulTitle);
        }
        else
        {
            builder.setTitle(R.string.importFailedTitle);
        }

        int messageId = success ? R.string.importedFrom : R.string.importFailed;

        final String template = getResources().getString(messageId);

        // Get the filename of the file being imported
        String filename = fileNameFromUri(path);
        if(filename == null)
        {
            filename = "(unknown)";
        }

        final String message = String.format(template, filename);
        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void onExportComplete(boolean success, final File path, final DataFormat format)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(success)
        {
            builder.setTitle(R.string.exportSuccessfulTitle);
        }
        else
        {
            builder.setTitle(R.string.exportFailedTitle);
        }

        int messageId = success ? R.string.exportedTo : R.string.exportFailed;

        final String template = getResources().getString(messageId);
        final String message = String.format(template, path.getAbsolutePath());
        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        if(success)
        {
            final CharSequence sendLabel = ImportExportActivity.this.getResources().getText(R.string.sendLabel);

            builder.setPositiveButton(sendLabel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Uri outputUri = FileProvider.getUriForFile(ImportExportActivity.this, BuildConfig.APPLICATION_ID, path);
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, outputUri);
                    sendIntent.setType(format.mimetype());

                    // set flag to give temporary permission to external app to use the FileProvider
                    sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    ImportExportActivity.this.startActivity(Intent.createChooser(sendIntent,
                            sendLabel));

                    dialog.dismiss();
                }
            });
        }

        builder.create().show();
    }

    /**
     * Determines if there is at least one activity that can perform the given intent
     */
    private boolean isCallable(Context context, final Intent intent)
    {
        PackageManager manager = context.getPackageManager();
        if(manager == null)
        {
            return false;
        }

        List<ResolveInfo> list = manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for(ResolveInfo info : list)
        {
            if(info.activityInfo.exported)
            {
                // There is one activity which is available to be called
                return true;
            }
        }

        return false;
    }

    private void chooseFileWithIntent(Intent intent)
    {
        try
        {
            startActivityForResult(intent, CHOOSE_EXPORT_FILE);
        }
        catch (ActivityNotFoundException e)
        {
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || requestCode != CHOOSE_EXPORT_FILE)
        {
            Log.w(TAG, "Failed onActivityResult(), result=" + resultCode);
            return;
        }

        Uri uri = data.getData();
        if(uri == null)
        {
            Log.e(TAG, "Activity returned a NULL URI");
            return;
        }

        try
        {
            InputStream reader = getContentResolver().openInputStream(uri);
            Log.e(TAG, "Starting file import with: " + uri.toString());
            startImport(null, reader, uri);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "Failed to import file: " + uri.toString(), e);
        }
    }
}
