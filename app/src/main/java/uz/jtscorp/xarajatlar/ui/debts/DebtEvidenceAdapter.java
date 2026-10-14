package uz.jtscorp.xarajatlar.ui.debts;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.DebtEvidence;
import uz.jtscorp.xarajatlar.utils.EvidenceFiles;

public class DebtEvidenceAdapter extends RecyclerView.Adapter<DebtEvidenceAdapter.ViewHolder> {

    private List<DebtEvidence> evidenceList = new ArrayList<>();
    private final OnEvidenceClickListener clickListener;
    private final OnDeleteClickListener deleteListener;

    public interface OnEvidenceClickListener {
        void onEvidenceClick(DebtEvidence evidence);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(DebtEvidence evidence);
    }

    public DebtEvidenceAdapter(OnEvidenceClickListener clickListener, OnDeleteClickListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void setEvidenceList(List<DebtEvidence> list) {
        this.evidenceList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<DebtEvidence> getEvidenceList() {
        return evidenceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_evidence, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DebtEvidence evidence = evidenceList.get(position);
        holder.bind(evidence);
    }

    @Override
    public int getItemCount() {
        return evidenceList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView evidenceImage;
        private final ImageView typeIcon;
        private final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            evidenceImage = itemView.findViewById(R.id.evidenceImage);
            typeIcon = itemView.findViewById(R.id.typeIcon);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(DebtEvidence evidence) {
            String type = evidence.getFileType();
            File file = EvidenceFiles.managedRegularFile(
                    itemView.getContext(), evidence.getFilePath());

            if ("image".equals(type) && file != null) {
                evidenceImage.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                typeIcon.setVisibility(View.GONE);
            } else if ("video".equals(type)) {
                evidenceImage.setImageResource(R.drawable.ic_video);
                typeIcon.setVisibility(View.GONE);
            } else if ("audio".equals(type)) {
                evidenceImage.setImageResource(R.drawable.ic_audio);
                typeIcon.setVisibility(View.GONE);
            } else {
                evidenceImage.setImageResource(R.drawable.ic_file);
                typeIcon.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onEvidenceClick(evidence);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(evidence);
                }
            });
        }
    }
}
