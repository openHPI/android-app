package de.xikolo.controllers.downloads;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import de.xikolo.GlobalApplication;
import de.xikolo.R;
import de.xikolo.utils.FileUtil;
import de.xikolo.utils.HeaderAndSectionsList;

public class DownloadsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = DownloadsAdapter.class.getSimpleName();

    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    private static final int ITEM_VIEW_TYPE_ITEM = 1;

    private HeaderAndSectionsList<String, List<FolderItem>> headerAndSectionsList;

    private OnDeleteButtonClickedListener callback;

    public DownloadsAdapter(OnDeleteButtonClickedListener callback) {
        this.callback = callback;
        this.headerAndSectionsList = new HeaderAndSectionsList<>();
    }

    public void addItem(String header, List<FolderItem> folder) {
        this.headerAndSectionsList.add(header, folder);
        notifyDataSetChanged();
    }

    public void clear() {
        this.headerAndSectionsList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return headerAndSectionsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (headerAndSectionsList.isHeader(position)) {
            return ITEM_VIEW_TYPE_HEADER;
        } else {
            return ITEM_VIEW_TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_header, parent, false);
            view.setEnabled(false);
            view.setOnClickListener(null);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
            view.setEnabled(false);
            view.setOnClickListener(null);
            return new FolderViewHolder(view);
        }
    }

    @SuppressWarnings("SetTextI18n")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder viewHolder = (HeaderViewHolder) holder;

            viewHolder.title.setText((String) headerAndSectionsList.getItem(position));
        } else {
            FolderViewHolder viewHolder = (FolderViewHolder) holder;

            final FolderItem folderItem = (FolderItem) headerAndSectionsList.getItem(position);

            Context context = GlobalApplication.getInstance();

            File dir = new File(folderItem.getPath());
            viewHolder.textTitle.setText(folderItem.getTitle().replaceAll("_", " "));

            long numberOfFiles = FileUtil.folderFileNumber(dir);

            viewHolder.textButtonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onDeleteButtonClicked(folderItem);
                }
            });

            if (numberOfFiles > 0) {
                viewHolder.textSubTitle.setText(numberOfFiles + " " + context.getString(R.string.files) + ": "
                        + FileUtil.getFormattedFileSize(FileUtil.folderSize(dir)));
                viewHolder.textButtonDelete.setVisibility(View.VISIBLE);
            } else {
                viewHolder.textSubTitle.setText(numberOfFiles + " " + context.getString(R.string.files));
                viewHolder.textButtonDelete.setVisibility(View.GONE);
            }

            if (position == getItemCount() - 1 || headerAndSectionsList.isHeader(position + 1)) {
                viewHolder.viewDivider.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.viewDivider.setVisibility(View.VISIBLE);
            }
        }
    }

    public static class FolderItem {

        private String title;

        private String path;

        public FolderItem(String title, String path) {
            this.title = title;
            this.path = path;
        }

        public String getTitle() {
            return title;
        }

        public String getPath() {
            return path;
        }

    }

    public interface OnDeleteButtonClickedListener {

        void onDeleteButtonClicked(FolderItem item);

    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {

        TextView textTitle;
        TextView textSubTitle;
        TextView textButtonDelete;
        View viewDivider;

        public FolderViewHolder(View itemView) {
            super(itemView);
            textTitle = (TextView) itemView.findViewById(R.id.textTitle);
            textSubTitle = (TextView) itemView.findViewById(R.id.textSubTitle);
            textButtonDelete = (TextView) itemView.findViewById(R.id.buttonDelete);
            viewDivider = itemView.findViewById(R.id.divider);
        }

    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.textHeader);
        }

    }

}
