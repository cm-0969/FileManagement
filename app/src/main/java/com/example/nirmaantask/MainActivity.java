package com.example.nirmaantask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private RecyclerView folderRecView;
    List<String> folderNameList;
    FirebaseStorage storage;
    String fName;
    String pdfName;
    TextView selectedPdfName;
    FolderGridAdapter adapter;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Button cancelButton;
    Button choosePdf;
    AlertDialog alertDialog;
    Intent intent;

    List<String> pdfNameList;
    List<Uri> uriList;
    List<String> downloadUrlList;//for uploading to firestore,will get after uploading to firebase storage


    int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        folderRecView = findViewById(R.id.fRecyclerView);
        folderRecView.setLayoutManager(new GridLayoutManager(this, 3));

        folderNameList = PrefConfig.readListFromPref(this);
        if (folderNameList == null)
            folderNameList = new ArrayList<>();

        adapter = new FolderGridAdapter(folderNameList);
        folderRecView.setAdapter(adapter);

        storage = FirebaseStorage.getInstance();

        pdfNameList = new ArrayList<>();
        uriList = new ArrayList<>();
        downloadUrlList = new ArrayList<>();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
                Log.d("fab", "onClick: ");
            }
        });

    }

    //FUNCTIONS

    private void showAlertDialog() {

        final AlertDialog.Builder docAlert = new AlertDialog.Builder(MainActivity.this);
        View alertView = getLayoutInflater().inflate(R.layout.doc_dialog, null);
        final EditText folderName = alertView.findViewById(R.id.folderName_eT);
        cancelButton = alertView.findViewById(R.id.dialog_cancel);
        Button createFolder = alertView.findViewById(R.id.createFolderbtn);
        choosePdf = alertView.findViewById(R.id.choosePdfbtn);
        selectedPdfName = alertView.findViewById(R.id.none_TV);

        docAlert.setView(alertView);
        alertDialog = docAlert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        choosePdf.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                //choose PDFs

                fName = folderName.getText().toString();
                Log.d("choosePdf", "onClick: ");
                if (fName.isEmpty()) {
                    folderName.setError("Please enter Folder name!");
                    folderName.requestFocus();
                    return;
                }

                intent = new Intent();
                intent.setType("application/pdf/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Please choose a PDF File"), 101);
                Log.d("size", "onClick: " + pdfNameList.size());

            }

        });


        createFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                uploadPDF(v);
                folderNameList.add(fName);
                PrefConfig.writeListInPref(getApplicationContext(), folderNameList);
                adapter.notifyDataSetChanged();
                alertDialog.dismiss();

            }

        });


        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();

    }


    private void uploadPDF(View v) {
        if (pdfNameList.size() != 0) {

            choosePdf.setEnabled(false);
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading Files...");
            progressDialog.setMessage("Uploaded 0/" + pdfNameList.size());
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();

            final StorageReference storageReference = storage.getReference();

            for (int i = 0; i < pdfNameList.size(); i++) {
                final int finalI = i;

                storageReference.child(fName + "/").child(pdfNameList.get(i)).putFile(uriList.get(i)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            storageReference.child(fName + "/").child(pdfNameList.get(finalI)).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    counter++;
                                    progressDialog.setMessage("Uploaded " + counter + "/" + pdfNameList.size());
                                    if (task.isSuccessful()) {
                                        downloadUrlList.add(task.getResult().toString());
                                    } else {
                                        storageReference.child(fName + "/").child(pdfNameList.get(finalI)).delete();
                                        Toast.makeText(MainActivity.this, "Sorry!, " + pdfNameList.get(finalI) + " could not be uploaded", Toast.LENGTH_SHORT).show();
                                    }
                                    if (counter == pdfNameList.size()) {
                                        saveFileDataToFirestore(progressDialog);
                                    }
                                }
                            });

                        } else {
                            progressDialog.setMessage("Uploaded " + counter + "/" + pdfNameList.size());
                            counter++;
                            Toast.makeText(MainActivity.this, pdfNameList.get(finalI) + " could not be uploaded!", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

            }

        } else {
            Toast.makeText(this, "Please add some PDF files!", Toast.LENGTH_SHORT).show();
        }
        counter = 0;
    }


    private void saveFileDataToFirestore(final ProgressDialog progressDialog) {


        Log.d("sizeTest", "saveFileDataToFirestore: " + downloadUrlList.size());
        progressDialog.setMessage("Saving uploaded files...");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < downloadUrlList.size(); i++) {
            DocumentReference documentReference = db.collection(fName).document(pdfNameList.get(i));
            map.put("url", downloadUrlList.get(i));
            map.put("name", pdfNameList.get(i));

            documentReference.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();

                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Firestore data upload failed", Toast.LENGTH_SHORT).show();
                            Log.e("error", "onFailure: " + e.getMessage());
                        }
                    });


        }
        pdfNameList.clear();
        uriList.clear();
        downloadUrlList.clear();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    pdfNameList.add(getFileName(uri));
                    uriList.add(uri);

                }
                selectedPdfName.setText(pdfNameList.toString());
            } else {
                Uri uri = data.getData();
                pdfNameList.add(getFileName(uri));
                uriList.add(uri);
                selectedPdfName.setText(pdfNameList.toString());
            }
        }

    }

    public String getFileName(Uri uri) {

        if (uri.toString().startsWith("content://")) {

            try {
                Cursor cursor = null;
                cursor = MainActivity.this.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    pdfName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.toString().startsWith("file://")) {

            pdfName = new File(uri.toString()).getName();
        } else {
            pdfName = uri.toString();
        }

        return pdfName;
    }


}

