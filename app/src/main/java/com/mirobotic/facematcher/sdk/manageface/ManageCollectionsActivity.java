
package com.mirobotic.facematcher.sdk.manageface;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cyberlink.facemedemo.ui.BaseActivity;
import com.cyberlink.facemedemo.ui.CLToast;
import com.mirobotic.facematcher.R;
import java.util.ArrayList;


public class ManageCollectionsActivity extends BaseActivity implements IManageCollectionListener, ManageHandler.ManageFaceHandlerCallback, IFetchFaceThumbnail {
    private static final String TAG = "ManageCollectionsActivity";
    public static final String KEY_FACE_COLLECTION_ID = "CollectionId";

    @Override
    public Bitmap OnFetchFaceThumbnail(long faceId) {
        Bitmap collectionBitmap = null;
        if (manageHandler != null) {
            collectionBitmap = manageHandler.getFaceThumbnail(faceId);
        }
        return collectionBitmap;
    }

    private ActionBar actionBar;
    private CollectionAdapter collectionAdapter;
    private ManageHandler manageHandler;
    private TextView emptyHint;
    private TextView loadingHint;
    private RecyclerView collectionListView;

    @Override
    protected String getTagId() {
        return TAG;
    }

    @LayoutRes
    @Override
    protected int getContentLayout() {
        return R.layout.activity_manage_collections;
    }

    @Override
    protected void initialize() {
        initToolBar();
        initCollectionList();
    }

    private void initToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
    }

    private void initManagerFaceHandler() {
        if (manageHandler != null) manageHandler.releaseEngine();
        manageHandler = new ManageHandler(this, this);
    }

    @Override
    public void onInitEngine(boolean success) {
        if (success) {
            updateCollections();
        }
    }

    private void initCollectionList() {
        emptyHint = findViewById(R.id.emptyHint);
        loadingHint = findViewById(R.id.loadingHint);
        collectionListView = findViewById(R.id.collectionList);

        collectionAdapter = new CollectionAdapter(this, this);
        collectionListView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        collectionAdapter.SetManageFaceCollectionListener(this);
        collectionListView.setAdapter(collectionAdapter);
    }

    @Override
    protected String[] getRequiredPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    protected String getPermissionString() {
        return "Storage";
    }

    @Override
    protected void onStart() {
        super.onStart();
        initManagerFaceHandler();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (manageHandler != null) {
            manageHandler.releaseEngine();
            manageHandler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
		FaceBitmapCache.getInstance().cleanCache();
    }

    @Override
    public void onEditCollection(CollectionHolder collectionHolder) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View editView = factory.inflate(R.layout.view_edit_face, null);
        ImageView faceThumbnail = editView.findViewById(R.id.faceThumbnail);
        EditText editText = editView.findViewById(R.id.txtName);
        editText.setText(collectionHolder.collectionName, TextView.BufferType.EDITABLE);
        Bitmap thumbnail = null;
        if (collectionHolder.faceIds.size() > 0) {
            thumbnail = manageHandler.getFaceThumbnail(collectionHolder.faceIds.get(0));
        }

        if (thumbnail == null) {
            faceThumbnail.setImageResource(R.drawable.ic_face_n);
        } else {
            faceThumbnail.setImageBitmap(thumbnail);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Update Collection")
                .setView(editView)
                .setCancelable(true)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    manageHandler.updateCollectionName(collectionHolder.collectionId, newName);
                    updateCollections();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                });
        builder.show();
    }

    @Override
    public void onDeleteCollection(CollectionHolder collectionHolder) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View thumbnailView = factory.inflate(R.layout.view_face_thumbnail, null);
        ImageView faceThumbnail = thumbnailView.findViewById(R.id.faceThumbnail);
        Bitmap thumbnail = null;
        if (collectionHolder.faceIds.size() > 0) {
            thumbnail = manageHandler.getFaceThumbnail(collectionHolder.faceIds.get(0));
        }
        if (thumbnail == null) {
            faceThumbnail.setImageResource(R.drawable.ic_face_n);
        } else {
            faceThumbnail.setImageBitmap(thumbnail);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Delete " + collectionHolder.collectionName + " ?")
                .setView(thumbnailView)
                .setPositiveButton("OK", (dialog, which) -> {
                    boolean result = manageHandler.deleteCollection(collectionHolder.collectionId);
                    if (result) {
                        CLToast.show(this, "Delete success");
                        updateCollections();
                    } else {
                        CLToast.show(this, "Delete failed");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                });
        builder.show();
    }

    @Override
    public void onClickCollection(CollectionHolder collectionHolder) {
        Intent intent = new Intent(getApplicationContext(), ManageFacesActivity.class);
        intent.putExtra(KEY_FACE_COLLECTION_ID, collectionHolder.collectionId);
        startActivity(intent);
    }

    private void updateCollections() {
        loadingHint.setVisibility(View.VISIBLE);
        if(manageHandler != null) {
            ArrayList<Long> collectionIds = manageHandler.getAllFaceCollections();
            ArrayList<CollectionHolder> collectionHolders = new ArrayList();
            if (collectionIds.size() > 0) {
                CollectionHolder holder;
                for (int i = 0; i < collectionIds.size(); i++) {
                    holder = manageHandler.getCollectionInfo(collectionIds.get(i));
                    if (holder != null) {
                        collectionHolders.add(holder);
                    }
                }
            }
            collectionAdapter.replaceAll(collectionHolders);
            if (collectionHolders.size() > 0) {
                emptyHint.setVisibility(View.GONE);
                collectionListView.setVisibility(View.VISIBLE);
            } else {
                emptyHint.setVisibility(View.VISIBLE);
                collectionListView.setVisibility(View.GONE);
            }
        }
        updateTitle();
        loadingHint.setVisibility(View.GONE);
    }

    private void updateTitle()
    {
        actionBar.setTitle("Face Collections (" + collectionAdapter.getItemCount() + ")");
    }
}
