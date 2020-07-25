package com.example.nirmaantask;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    List<String> downloadUrlList;
    List<String> pdfNameList;
    StorageReference storageRef;
    String folder_name;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


    public PdfAdapter(List<String> pdfUrlList, List<String> pdfNameList, String folder_name) {
        this.downloadUrlList = pdfUrlList;
        this.pdfNameList = pdfNameList;
        this.folder_name = folder_name;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pdf_view, parent, false);
        storageRef = FirebaseStorage.getInstance().getReference();

        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, final int position) {

        holder.pdfDisplayName.setText(pdfNameList.get(position));
    }

    @Override
    public int getItemCount() {
        return pdfNameList.size();
    }

    public class PdfViewHolder extends RecyclerView.ViewHolder {

        TextView pdfDisplayName;

        public PdfViewHolder(@NonNull final View itemView) {
            super(itemView);

            pdfDisplayName = itemView.findViewById(R.id.pdfDisplayName_tv);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setType(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(downloadUrlList.get(getAdapterPosition())));
                    v.getContext().startActivity(intent);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final Dialog dialog = new Dialog(itemView.getContext());
                    dialog.setContentView(R.layout.pdfdel_dialog);
                    dialog.setTitle("Delete File");

                    TextView text = dialog.findViewById(R.id.del_text);
                    text.setText(R.string.pdf_del_prompt);

                    Button cancelDelete = dialog.findViewById(R.id.pdf_delCancel);
                    Button confirmDelete = dialog.findViewById(R.id.pdf_delConfirm);

                    cancelDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    confirmDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final ProgressDialog progressDialog = new ProgressDialog(itemView.getContext());

                            progressDialog.setMessage("Deleting File...");
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                            progressDialog.show();

                            StorageReference pdfRef = storageRef.getStorage().getReferenceFromUrl(downloadUrlList.get(getAdapterPosition()));
                            pdfRef.delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            db.collection(folder_name).document(pdfNameList.get(getAdapterPosition()))
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d("delTest", "onSuccess: ");
                                                            progressDialog.dismiss();
                                                            dialog.dismiss();
                                                            Snackbar.make(itemView, "File deleted Successfully!", Snackbar.LENGTH_SHORT).show();
                                                            pdfNameList.remove(getAdapterPosition());
                                                            downloadUrlList.remove(getAdapterPosition());
                                                            notifyItemRemoved(getAdapterPosition());

                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d("delFail", "onFailure: " + e.getLocalizedMessage());
                                                            progressDialog.dismiss();
                                                            dialog.dismiss();
                                                        }
                                                    });

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                        }
                                    });
                        }
                    });
                    dialog.show();

                    return true;
                }
            });
        }
    }
}
