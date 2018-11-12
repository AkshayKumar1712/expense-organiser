package halif.expenseorganiser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransactionViewActivity extends AppCompatActivity {
    private static final String TAG = "BudgetWatch";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSIONS_REQUEST_CAMERA = 2;

    static final String ACTION_NEW_EXPENSE = "ActionAddExpense";
    static final String ACTION_NEW_REVENUE = "ActionAddRevenue";

    private String capturedUncommittedReceipt = null;
    private DBHelper _db;

    private EditText _nameEdit;
    private TextView _nameView;
    private EditText _accountEdit;
    private TextView _accountView;
    private EditText _valueEdit;
    private TextView _valueView;
    private EditText _noteEdit;
    private TextView _noteView;
    private TextView _budgetView;
    private TextView _dateView;
    private Button _captureButton;
    private Button _viewButton;
    private Button _updateButton;
    private View _receiptLayout;
    private View _endingDivider;
    private TextView _receiptLocationField;
    private View _noReceiptButtonLayout;
    private View _hasReceiptButtonLayout;
    private EditText _dateEdit;
    private Spinner _budgetSpinner;

    private int _transactionId;
    private int _type;
    private boolean _updateTransaction;
    private boolean _addTransaction;
    private String parentBudget;

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        String action = intent.getAction();
        if (b != null) {
            _transactionId = b.getInt("id");
            _type = b.getInt("type");
            _updateTransaction = b.getBoolean("update", false);
            _addTransaction = b.getBoolean("add", false);
            _nameEdit.setText(b.getString("name", ""));
            _valueEdit.setText(b.getString("value", ""));
            parentBudget = b.getString("parentBudget");
        } else if (action != null) {
            _updateTransaction = false;
            if (action.equals(ACTION_NEW_EXPENSE)) {
                _type = DBHelper.TransactionDbIds.EXPENSE;
            } else if (action.equals(ACTION_NEW_REVENUE)) {
                _type = DBHelper.TransactionDbIds.REVENUE;
            } else {
                Log.d(TAG, "Unsupported action '" + action + "', bailing");
                finish();
            }
        } else {
            Log.d(TAG, "Launched TransactionViewActivity without arguments, bailing");
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.transaction_view_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        _db = new DBHelper(this);

        _nameEdit = findViewById(R.id.nameEdit);
        _nameView = findViewById(R.id.nameView);
        _accountEdit = findViewById(R.id.accountEdit);
        _accountView = findViewById(R.id.accountView);
        _valueEdit = findViewById(R.id.valueEdit);
        _valueView = findViewById(R.id.valueView);
        _noteEdit = findViewById(R.id.noteEdit);
        _noteView = findViewById(R.id.noteView);
        _budgetView = findViewById(R.id.budgetView);
        _dateView = findViewById(R.id.dateView);
        _captureButton = findViewById(R.id.captureButton);
        _viewButton = findViewById(R.id.viewButton);
        _updateButton = findViewById(R.id.updateButton);
        _receiptLayout = findViewById(R.id.receiptLayout);
        _endingDivider = findViewById(R.id.endingDivider);
        _receiptLocationField = findViewById(R.id.receiptLocation);
        _noReceiptButtonLayout = findViewById(R.id.noReceiptButtonLayout);
        _hasReceiptButtonLayout = findViewById(R.id.hasReceiptButtonLayout);
        _dateEdit = findViewById(R.id.dateEdit);
        _budgetSpinner = findViewById(R.id.budgetSpinner);

        extractIntentFields(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onResume() {
        super.onResume();

        if (_type == DBHelper.TransactionDbIds.EXPENSE) {
            if (_updateTransaction) {
                setTitle(R.string.editExpenseTransactionTitle);
            } else {
                setTitle(R.string.addExpenseTransactionTitle);
            }
        } else if (_type == DBHelper.TransactionDbIds.REVENUE) {
            if (_updateTransaction) {
                setTitle(R.string.editRevenueTransactionTitle);
            } else {
                setTitle(R.string.addRevenueTransactionTitle);
            }
        }

        final Calendar date = new GregorianCalendar();
        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();

        _dateEdit.setText(dateFormatter.format(date.getTime()));

        final DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            date.set(year, month, day);
            _dateEdit.setText(dateFormatter.format(date.getTime()));
        };

        _dateEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                int year = date.get(Calendar.YEAR);
                int month = date.get(Calendar.MONTH);
                int day = date.get(Calendar.DATE);
                DatePickerDialog datePicker = new DatePickerDialog(TransactionViewActivity.this,
                        dateSetListener, year, month, day);
                datePicker.show();
            }
        });

        List<String> actualBudgetNames = _db.getBudgetNames();
        LinkedList<String> budgetNames = new LinkedList<>(actualBudgetNames);

        // A user is not allowed to create a blank budget. However, here
        // an empty budget is injected, so that a user can create a
        // transaction that has no budget.
        budgetNames.addFirst("");

        // Add budget items to spinner if it has not been initialized yet
        if (_budgetSpinner.getCount() == 0) {
            ArrayAdapter<String> budgets = new ArrayAdapter<>(this, R.layout.spinner_textview, budgetNames);
            _budgetSpinner.setAdapter(budgets);

            if (_addTransaction) {
                _budgetSpinner.setSelection(budgets.getPosition(parentBudget));
            }
        }

        if (_updateTransaction) {
            Transaction transaction = _db.getTransaction(_transactionId);
            (_updateTransaction ? _nameEdit : _nameView).setText(transaction.description);
            (_updateTransaction ? _accountEdit : _accountView).setText(transaction.account);

            int budgetIndex = budgetNames.indexOf(transaction.budget);
            if (budgetIndex >= 0) {
                _budgetSpinner.setSelection(budgetIndex);
            }
            (_updateTransaction ? _valueEdit : _valueView).setText(String.format(Locale.US, "%.2f", transaction.value));
            (_updateTransaction ? _noteEdit : _noteView).setText(transaction.note);
            (_updateTransaction ? _dateEdit : _dateView).setText(dateFormatter.format(new Date(transaction.dateMs)));
            _receiptLocationField.setText(transaction.receipt);

        } else {
            _budgetView.setVisibility(View.GONE);
            _nameView.setVisibility(View.GONE);
            _accountView.setVisibility(View.GONE);
            _valueView.setVisibility(View.GONE);
            _noteView.setVisibility(View.GONE);
            _dateView.setVisibility(View.GONE);

            // If adding a transaction, always list the receipt field
            _receiptLayout.setVisibility(View.VISIBLE);
            _endingDivider.setVisibility(View.VISIBLE);
            if (capturedUncommittedReceipt == null) {
                _noReceiptButtonLayout.setVisibility(View.VISIBLE);
                _hasReceiptButtonLayout.setVisibility(View.GONE);
            } else {
                _noReceiptButtonLayout.setVisibility(View.GONE);
                _hasReceiptButtonLayout.setVisibility(View.VISIBLE);
                _updateButton.setVisibility(View.VISIBLE);
            }
        }

        View.OnClickListener captureCallback = v -> {
            if (ContextCompat.checkSelfPermission(TransactionViewActivity.this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                captureReceipt();
            } else {
                ActivityCompat.requestPermissions(TransactionViewActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_REQUEST_CAMERA);
            }
        };

        _captureButton.setOnClickListener(captureCallback);
        _updateButton.setOnClickListener(captureCallback);

        _viewButton.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), ReceiptViewActivity.class);
            final Bundle b = new Bundle();

            final TextView receiptField = findViewById(R.id.receiptLocation);

            String receipt = receiptField.getText().toString();
            if (capturedUncommittedReceipt != null) {
                receipt = capturedUncommittedReceipt;
            }

            b.putString("receipt", receipt);
            i.putExtras(b);
            startActivity(i);
        });
    }

    private void doSave() {
        final String name = _nameEdit.getText().toString();
        // name field is optional, so it is OK if it is empty

        final String budget = (String) _budgetSpinner.getSelectedItem();
        if (budget == null) {
            Snackbar.make(_budgetSpinner, R.string.budgetMissing, Snackbar.LENGTH_LONG).show();
            return;
        }

        final String account = _accountEdit.getText().toString();
        // The account field is optional, so it is OK if it is empty

        final String valueStr = _valueEdit.getText().toString();
        if (valueStr.isEmpty()) {
            Snackbar.make(_valueEdit, R.string.valueMissing, Snackbar.LENGTH_LONG).show();
            return;
        }

        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            Snackbar.make(_valueEdit, R.string.valueInvalid, Snackbar.LENGTH_LONG).show();
            return;
        }

        final String note = _noteEdit.getText().toString();
        // The note field is optional, so it is OK if it is empty

        final String dateStr = _dateEdit.getText().toString();
        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
        long dateMs;
        try {
            dateMs = dateFormatter.parse(dateStr).getTime();
        } catch (ParseException e) {
            Snackbar.make(_dateEdit, R.string.dateInvalid, Snackbar.LENGTH_LONG).show();
            return;
        }

        String receipt = _receiptLocationField.getText().toString();
        if (capturedUncommittedReceipt != null) {
            // Delete the old receipt, it is no longer needed
            File oldReceipt = new File(receipt);
            if (oldReceipt.delete() == false) {
                Log.e(TAG, "Unable to delete old receipt file: " + capturedUncommittedReceipt);
            }

            // Remember the new receipt to save
            receipt = capturedUncommittedReceipt;
            capturedUncommittedReceipt = null;
        }

        if (_updateTransaction) {
            _db.updateTransaction(_transactionId, _type, name, account,
                    budget, value, note, dateMs, receipt);

        } else {
            _db.insertTransaction(_type, name, account, budget,
                    value, note, dateMs, receipt);
        }

        finish();
    }

    private void captureReceipt() {
        if (capturedUncommittedReceipt != null) {
            Log.i(TAG, "Deleting unsaved image: " + capturedUncommittedReceipt);
            File unneededReceipt = new File(capturedUncommittedReceipt);
            if (unneededReceipt.delete() == false) {
                Log.e(TAG, "Unable to delete unnecessary file: " + capturedUncommittedReceipt);
            }
            capturedUncommittedReceipt = null;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            Log.e(TAG, "Failed to get package manager, cannot take picture");
            Toast.makeText(getApplicationContext(), R.string.pictureCaptureError,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (takePictureIntent.resolveActivity(packageManager) == null) {
            Log.e(TAG, "Could not find an activity to take a picture");
            Toast.makeText(getApplicationContext(), R.string.pictureCaptureError, Toast.LENGTH_LONG).show();
            return;
        }

        File imageLocation = getNewImageLocation();

        Uri imageUri;

        // Starting in Android N (24+) sharing a file Uri is discouraged or prevented.
        // For those platforms a FileProvider is used to provide a content Uri. Older
        // platforms still use the file Uri, in part to also allow easier testing
        // using Robolectric.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, imageLocation);
        } else {
            imageUri = Uri.fromFile(imageLocation);
        }

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        capturedUncommittedReceipt = (imageLocation != null ? imageLocation.getAbsolutePath() : null);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onDestroy() {
        if (capturedUncommittedReceipt != null) {
            // The receipt was captured but never used
            Log.i(TAG, "Deleting unsaved image: " + capturedUncommittedReceipt);
            File unneededReceipt = new File(capturedUncommittedReceipt);
            if (unneededReceipt.delete() == false) {
                Log.e(TAG, "Unable to delete unnecessary file: " + capturedUncommittedReceipt);
            }
            capturedUncommittedReceipt = null;
        }

        _db.close();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (_updateTransaction) {
            getMenuInflater().inflate(R.menu.edit_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.add_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_save: {
                doSave();
                return true;
            }
            case android.R.id.home: {
                finish();
                return true;
            }
            case R.id.action_edit: {
                finish();

                Intent i = new Intent(getApplicationContext(), TransactionViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", _transactionId);
                bundle.putInt("type", _type);
                bundle.putBoolean("update", true);
                i.putExtras(bundle);
                startActivity(i);
                return true;
            }
            case R.id.action_delete: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.deleteTransactionTitle);
                builder.setMessage(R.string.deleteTransactionConfirmation);
                builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                    Log.e(TAG, "Deleting transaction: " + _transactionId);

                    _db.deleteTransaction(_transactionId);
                    finish();

                    dialog.dismiss();
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        }
        return super.onOptionsItemSelected(item);

//        if (id == R.id.action_save) {
//            doSave();
//            return true;
//        }

//        if (id == android.R.id.home) {
//            finish();
//            return true;
//        }
//
//        if (id == R.id.action_edit) {
//            finish();
//
//            Intent i = new Intent(getApplicationContext(), TransactionViewActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putInt("id", _transactionId);
//            bundle.putInt("type", _type);
//            bundle.putBoolean("update", true);
//            i.putExtras(bundle);
//            startActivity(i);
//            return true;
//        }
//
//        if (id == R.id.action_delete) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.deleteTransactionTitle);
//            builder.setMessage(R.string.deleteTransactionConfirmation);
//            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
//                Log.e(TAG, "Deleting transaction: " + _transactionId);
//
//                _db.deleteTransaction(_transactionId);
//                finish();
//
//                dialog.dismiss();
//            });
//            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
//            AlertDialog dialog = builder.create();
//            dialog.show();
//
//            return true;
//        }

    }

    private File getNewImageLocation() {
        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (imageDir == null) {
            Log.e(TAG, "Failed to locate directory for pictures");
            Toast.makeText(this, R.string.pictureCaptureError, Toast.LENGTH_LONG).show();
            return null;
        }

        if (imageDir.exists() == false) {
            if (imageDir.mkdirs() == false) {
                Log.e(TAG, "Failed to create receipts image directory");
                Toast.makeText(this, R.string.pictureCaptureError, Toast.LENGTH_LONG).show();
                return null;
            }
        }

        UUID imageFilename = UUID.randomUUID();
        File receiptFile = new File(imageDir, imageFilename.toString() + ".jpg");

        return receiptFile;
    }

    private boolean reencodeImageWithQuality(String original, int quality) {
        File destFile = new File(original);
        File tmpLocation = getNewImageLocation();

        try {
            if (tmpLocation == null) {
                throw new IOException("Could not create location for tmp file");
            }

            boolean created = tmpLocation.createNewFile();
            if (created == false) {
                throw new IOException("Could not create tmp file");
            }

            Bitmap bitmap = BitmapFactory.decodeFile(original);
            FileOutputStream fOut = new FileOutputStream(tmpLocation);
            boolean fileWritten = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fOut);
            fOut.flush();
            fOut.close();

            if (fileWritten == false) {
                throw new IOException("Could not down compress file");
            }

            boolean renamed = tmpLocation.renameTo(destFile);
            if (renamed == false) {
                throw new IOException("Could not move converted file");
            }

            Log.i(TAG, "Image file " + original + " saved at quality " + quality);

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to encode image", e);

            for (File item : new File[]{tmpLocation, destFile}) {
                if (item != null) {
                    boolean result = item.delete();
                    if (result == false) {
                        Log.w(TAG, "Failed to delete image file: " + item.getAbsolutePath());
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Received image from camera");

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            String jpegQualityLevelStr = prefs.getString("jpegQuality", "");
            int jpegQualityLevel = 40; // default value

            try {
                jpegQualityLevel = Integer.parseInt(jpegQualityLevelStr);
            } catch (NumberFormatException e) {
                // If the setting has no value or is otherwise invalid, fall back
                // on a default value
            }

            final int JPEG_QUALITY_LEVEL = jpegQualityLevel;

            if (resultCode != RESULT_OK || JPEG_QUALITY_LEVEL == 100) {
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "Failed to create receipt image: " + resultCode);
                    // No image was actually created, simply forget the patch
                    capturedUncommittedReceipt = null;
                } else {
                    Log.i(TAG, "Image file saved: " + capturedUncommittedReceipt);
                }

                onResume();
            } else {
                Log.i(TAG, "Re-encoding image in background");

                AsyncTask<Void, Void, Boolean> imageConverter = new AsyncTask<Void, Void, Boolean>() {
                    ProgressDialog dialog;

                    @Override
                    protected void onPreExecute() {
                        dialog = new ProgressDialog(TransactionViewActivity.this);
                        dialog.setMessage(TransactionViewActivity.this.getResources().getString(R.string.encodingReceipt));
                        dialog.setCancelable(false);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.show();
                    }

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        return reencodeImageWithQuality(capturedUncommittedReceipt, JPEG_QUALITY_LEVEL);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (result != null && result) {
                            Log.i(TAG, "Image file re-encoded: " + capturedUncommittedReceipt);
                        } else {
                            Log.e(TAG, "Failed to re-encode image");
                            // No image was actually created, simply forget the patch
                            capturedUncommittedReceipt = null;
                        }

                        dialog.hide();
                        TransactionViewActivity.this.onResume();
                    }
                };

                imageConverter.execute();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted.
                captureReceipt();
            } else {
                // Camera permission rejected, inform user that
                // no receipt can be taken.
                Toast.makeText(getApplicationContext(), R.string.noCameraPermissionError,
                        Toast.LENGTH_LONG).show();
            }

        }
    }
}