package uz.jtscorp.xarajatlar.ui.settings;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;

public class FamilyMemberAdapter extends ListAdapter<FamilyMember, FamilyMemberAdapter.ViewHolder> {

    private static final int[] COLORS = {
            0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0,
            0xFFE91E63, 0xFF00BCD4, 0xFF795548, 0xFF607D8B
    };

    private final OnItemClickListener onItemClick;
    private final OnDeleteClickListener onDeleteClick;

    public interface OnItemClickListener {
        void onItemClick(FamilyMember member);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(FamilyMember member);
    }

    public FamilyMemberAdapter(OnItemClickListener onItemClick, OnDeleteClickListener onDeleteClick) {
        super(DIFF_CALLBACK);
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
    }

    private static final DiffUtil.ItemCallback<FamilyMember> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FamilyMember>() {
                @Override
                public boolean areItemsTheSame(@NonNull FamilyMember oldItem, @NonNull FamilyMember newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull FamilyMember oldItem, @NonNull FamilyMember newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            (oldItem.getEmoji() == null ? newItem.getEmoji() == null :
                                    oldItem.getEmoji().equals(newItem.getEmoji()));
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = getItem(position);
        holder.bind(member);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View colorIndicator;
        private final TextView emojiText;
        private final TextView titleText;
        private final TextView subtitleText;
        private final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            emojiText = itemView.findViewById(R.id.emojiText);
            titleText = itemView.findViewById(R.id.titleText);
            subtitleText = itemView.findViewById(R.id.subtitleText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(FamilyMember member) {
            titleText.setText(member.getName());
            subtitleText.setVisibility(View.GONE);

            // Set emoji or first letter
            String emoji = member.getEmoji();
            if (emoji != null && !emoji.isEmpty()) {
                emojiText.setText(emoji);
                emojiText.setVisibility(View.VISIBLE);
            } else {
                // Show first letter of name as fallback
                String name = member.getName();
                if (name != null && !name.isEmpty()) {
                    emojiText.setText(name.substring(0, 1).toUpperCase());
                }
                emojiText.setVisibility(View.VISIBLE);
            }

            // Set color based on position
            int color = COLORS[getAdapterPosition() % COLORS.length];
            GradientDrawable drawable = (GradientDrawable) colorIndicator.getBackground();
            drawable.setColor(color);

            itemView.setOnClickListener(v -> onItemClick.onItemClick(member));
            deleteButton.setOnClickListener(v -> onDeleteClick.onDeleteClick(member));
        }
    }
}
