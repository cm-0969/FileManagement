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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewDocActivity extends AppCompatActivity {

    private TextView fName;
    private RecyclerView pdfRecView;
    PdfAdapter adapter;
    List<String> downloadUrlList;
    List<String> pdfNameList;

    String folder_name;
    String pdfName;
    String pdfUrl;
    ProgressBar progressBar;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    Button cancelBtn;
    Button uploadFile;
    Button chooseFiles;
    AlertDialog alertDialog;
    Intent i;

    List<String> fileNameList;
    List<Uri> uriList;
    List<String> downloadList;

    TextView selectedFile;
    String fileName;
    int counter;

    FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_doc);

        progressBar = findViewById(R.id.progressRec);
        progressBar.setVisibility(View.VISIBLE);

        Intent intent = getIntent();

        folder_name = intent.getStringExtra("folderClicked");
        fName = findViewById(R.id.fName_tv);
        fName.setText(folder_name);

        pdfRecView = findViewById(R.id.pdfRecyclerView);
        pdfRecView.setLayoutManager(new GridLayoutManager(this, 3));

        downloadUrlList = new ArrayList<>();
        pdfNameList = new ArrayList<>();

        fileNameList = new ArrayList<>();
        uriList = new ArrayList<>();
        downloadList = new ArrayList<>();

        storage = FirebaseStorage.getInstance();

        db.collection(folder_name)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                pdfName = document.getString("name");
                                pdfUrl = document.getString("url");

                                Log.d("nameTest", "onComplete: " + pdfName);
                                Log.d("urlTest", "onComplete: " + pdfUrl);
                                pdfNameList.add(pdfName);
                                downloadUrlList.add(pdfUrl);

                            }
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

        adapter = new PdfAdapter(downloadUrlList, pdfNameList, folder_name);
        pdfRecView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabPdf);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });


    }

    private void showDialog() {
        final AlertDialog.Builder docAlert = new AlertDialog.Builder(ViewDocActivity.this);
        View alertView = getLayoutInflater().inflate(R.layout.add_pdf_dialog, null);
        cancelBtn = alertView.findViewById(R.id.cancelPDFdialog);
        uploadFile = alertView.findViewById(R.id.uploadFiles);
        chooseFiles = alertView.findViewById(R.id.fileChoose);
        selectedFile = alertView.findViewById(R.id.noneText);

        docAlert.setView(alertView);
        alertDialog = docAlert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        chooseFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                i = new Intent();
                i.setType("application/pdf/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(i, "Please choose a PDF File"), 1010);

            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPDF(v);
                adapter.notifyDataSetChanged();
                alertDialog.dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();

    }

    private void uploadPDF(View v) {

        if (fileNameList.size() != 0) {
            chooseFiles.setEnabled(false);
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading Files...");
            progressDialog.setMessage("Uploaded 0/" + fileNameList.size());
            progressDialog.setCanceledOnTouchOutside(false); //Remove this line if you want your user to be able to cancel upload
            progressDialog.setCancelable(false);    //Remove this line if you want your user to be able to cancel upload
            progressDialog.show();

            final StorageReference storageReference = storage.getReference();

            for (int i = 0; i < fileNameList.size(); i++) {
                final int finalI = i;

                storageReference.child(folder_name + "/").child(fileNameList.get(i)).putFile(uriList.get(i)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            storageReference.child(folder_name + "/").child(fileNameList.get(finalI)).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    counter++;
                                    progressDialog.setMessage("Uploaded " + counter + "/" + fileNameList.size());
                                    if (task.isSuccessful()) {
                                        downloadList.add(task.getResult().toString());

                                    } else {
                                        storageReference.child(fName + "/").child(fileNameList.get(finalI)).delete();
                                        Toast.makeText(ViewDocActivity.this, "Sorry!, " + fileNameList.get(finalI) + " could not be uploaded", Toast.LENGTH_SHORT).show();
                                    }
                                    if (counter == fileNameList.size()) {
                                        saveFileDataToFirestore(progressDialog);
                                    }
                                }
                            });

                        } else {
                            progressDialog.setMessage("Uploaded " + counter + "/" + fileNameList.size());
                            counter++;
                            Toast.makeText(ViewDocActivity.this, fileNameList.get(finalI) + " could not be uploaded!", Toast.LENGTH_SHORT).show();
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

        Log.d("docSizeTest", "saveFileDataToFirestore: " + downloadList.size());

        progressDialog.setMessage("Saving uploaded files...");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < downloadList.size(); i++) {
            DocumentReference documentReference = db.collection(folder_name).document(fileNameList.get(i));
            map.put("url", downloadList.get(i));
            map.put("name", fileNameList.get(i));

            pdfNameList.add(fileNameList.get(i));
            downloadUrlList.add(downloadList.get(i));
            adapter.notifyDataSetChanged();

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
                            Toast.makeText(ViewDocActivity.this, "Firestore data upload failed", Toast.LENGTH_SHORT).show();
                            Log.e("error", "onFailure: " + e.getMessage());
                        }
                    });


        }

        fileNameList.clear();
        uriList.clear();
        downloadList.clear();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1010 && resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    fileNameList.add(getPdfName(uri));
                    uriList.add(uri);

                }
                selectedFile.setText(fileNameList.toString());
            } else {
                Uri uri = data.getData();
                fileNameList.add(getPdfName(uri));
                uriList.add(uri);
                selectedFile.setText(fileNameList.toString());
            }
        }

    }

    public String getPdfName(Uri uri) {

        if (uri.toString().startsWith("content://")) {

            try {
                Cursor cursor = null;
                cursor = ViewDocActivity.this.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.toString().startsWith("file://")) {

            fileName = new File(uri.toString()).getName();
        } else {
            fileName = uri.toString();
        }

        return fileName;
    }

}