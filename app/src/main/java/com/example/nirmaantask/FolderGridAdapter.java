package com.example.nirmaantask;

import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class FolderGridAdapter extends RecyclerView.Adapter<FolderGridAdapter.FolderViewHolder> {

    List<String> folderNameList;


    public FolderGridAdapter(List<String> folderNameList) {
        this.folderNameList = folderNameList;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_view, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, final int position) {

        holder.fName.setText(folderNameList.get(position));

    }

    @Override
    public int getItemCount() {
        return folderNameList.size();
    }

    public class FolderViewHolder extends RecyclerView.ViewHolder {

        TextView fName;

        public FolderViewHolder(@NonNull final View itemView) {
            super(itemView);

            fName = itemView.findViewById(R.id.folderName_textView);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), ViewDocActivity.class);
                    intent.putExtra("folderClicked", folderNameList.get(getAdapterPosition()));
                    v.getContext().startActivity(intent);

                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final Dialog dialog = new Dialog(itemView.getContext());
                    dialog.setContentView(R.layout.delete_dialog);
                    dialog.setTitle("Delete Folder");

                    TextView text = dialog.findViewById(R.id.dText);
                    text.setText(R.string.delete_prompt);

                    Button cancelDelete = dialog.findViewById(R.id.cancelDeletebtn);

                    cancelDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    Button confirmDelete = dialog.findViewById(R.id.delete_confirmbtn);

                    confirmDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            folderNameList.remove(getAdapterPosition());
                            notifyItemRemoved(getAdapterPosition());
                            PrefConfig.deletePref(itemView.getContext(), folderNameList);
                            PrefConfig.writeListInPref(itemView.getContext(), folderNameList);
                            dialog.dismiss();
                            Log.d("delTest", "onClick: " + folderNameList.size());
                        }
                    });

                    dialog.show();


                    return true;
                }
            });
        }
    }


}
