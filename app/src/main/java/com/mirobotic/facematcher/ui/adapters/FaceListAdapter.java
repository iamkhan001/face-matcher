package com.mirobotic.facematcher.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberlink.facemedemo.data.SavedFace;
import com.mirobotic.facematcher.R;

import java.io.File;
import java.util.ArrayList;

public class FaceListAdapter extends RecyclerView.Adapter<FaceListAdapter.MyViewHolder> {

    private ArrayList<SavedFace> list;

    public FaceListAdapter(ArrayList<SavedFace> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_face,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        SavedFace face = list.get(position);

        if (face.getBitmap()!=null){
            holder.imgFace.setImageBitmap(face.getBitmap());
        }


        holder.tvName.setText(face.getName());

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        private ImageView imgFace;
        private TextView tvName;
        MyViewHolder(@NonNull View itemView) {
            super(itemView);

            imgFace = itemView.findViewById(R.id.imgFace);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }


}
