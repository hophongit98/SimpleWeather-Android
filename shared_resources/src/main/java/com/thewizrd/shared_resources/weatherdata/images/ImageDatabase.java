package com.thewizrd.shared_resources.weatherdata.images;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.firebase.FirebaseHelper;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ImageDatabase {
    private static final String TAG = "ImageDatabase";

    @WorkerThread
    public static List<ImageData> getAllImageDataForCondition(final String backgroundCode) {
        return AsyncTask.await(new Callable<List<ImageData>>() {
            @Override
            public List<ImageData> call() {
                FirebaseFirestore db = FirebaseHelper.getFirestoreDB();
                Query query = db.collection("background_images")
                        .whereEqualTo("condition", backgroundCode);

                QuerySnapshot querySnapshot = null;
                try {
                    // Try to retrieve from cache first
                    if (!ImageDataHelper.shouldInvalidateCache()) {
                        querySnapshot = Tasks.await(query.get(Source.CACHE));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    querySnapshot = null;
                }

                // If data is missing from cache, get data from server
                if (querySnapshot == null) {
                    try {
                        querySnapshot = Tasks.await(query.get(Source.SERVER));

                        // Run query to cache data
                        saveSnapshot(db);
                    } catch (ExecutionException | InterruptedException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                }

                ArrayList<ImageData> list = new ArrayList<>();
                try {
                    if (querySnapshot != null) {
                        list.ensureCapacity(querySnapshot.getDocuments().size());
                        for (DocumentSnapshot docSnapshot : querySnapshot.getDocuments()) {
                            if (docSnapshot.exists()) {
                                list.add(docSnapshot.toObject(ImageData.class));
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.writeLine(Log.ERROR, e);
                }

                return list;
            }
        });
    }

    @WorkerThread
    public static ImageData getRandomImageForCondition(final String backgroundCode) {
        return AsyncTask.await(new Callable<ImageData>() {
            @Override
            public ImageData call() {
                FirebaseFirestore db = FirebaseHelper.getFirestoreDB();
                Query query = db.collection("background_images")
                        .whereEqualTo("condition", backgroundCode);

                QuerySnapshot querySnapshot = null;
                try {
                    // Try to retrieve from cache first
                    if (!ImageDataHelper.shouldInvalidateCache()) {
                        querySnapshot = Tasks.await(query.get(Source.CACHE));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    querySnapshot = null;
                }

                // If data is missing from cache, get data from server
                if (querySnapshot == null || !querySnapshot.iterator().hasNext()) {
                    try {
                        querySnapshot = Tasks.await(query.get(Source.SERVER));

                        // Run query to cache data
                        saveSnapshot(db);
                    } catch (ExecutionException | InterruptedException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                }

                final Random rand = new Random();
                if (querySnapshot != null) {
                    List<DocumentSnapshot> collection = querySnapshot.getDocuments();
                    Collections.sort(collection, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot o1, DocumentSnapshot o2) {
                            return rand.nextInt(1 - (-1)) + -1;
                        }
                    });

                    if (collection.size() > 0) {
                        DocumentSnapshot docSnapshot = collection.get(0);
                        ImageData imageData = null;
                        try {
                            imageData = docSnapshot.toObject(ImageData.class);
                        } catch (Exception e) {
                            Logger.writeLine(Log.ERROR, e);
                        }
                        return imageData;
                    }
                }

                return null;
            }
        });
    }

    @WorkerThread
    private static void saveSnapshot(@NonNull final FirebaseFirestore db) {
        AnalyticsLogger.logEvent(TAG + ": saveSnapshot");

        AsyncTask.await(new Callable<Void>() {
            @Override
            public Void call() {
                // Get all data from server to cache locally
                Query query = db.collection("background_images");
                try {
                    Tasks.await(query.get(Source.SERVER));
                    ImageDataHelper.invalidateCache(false);
                    if (ImageDataHelper.getImageDBUpdateTime() == 0) {
                        ImageDataHelper.setImageDBUpdateTime(Tasks.await(ImageDatabase.getLastUpdateTime()));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Logger.writeLine(Log.ERROR, e);
                }

                // Register worker
                ApplicationLib app = SimpleLibrary.getInstance().getApp();
                if (app.isPhone()) {
                    LocalBroadcastManager.getInstance(app.getAppContext())
                            .sendBroadcast(new Intent(CommonActions.ACTION_IMAGES_UPDATEWORKER));
                }
                return null;
            }
        });
    }

    @WorkerThread
    public static Task<Long> getLastUpdateTime() {
        final TaskCompletionSource<Long> tcs = new TaskCompletionSource<>();

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                FirebaseDatabase db = FirebaseHelper.getFirebaseDB();
                DatabaseReference ref = db.getReference().child("background_images_info")
                        .child("collection_info").child("last_updated");
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long lastUpdated = snapshot.getValue(Long.class);
                        tcs.setResult(lastUpdated);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tcs.setResult(0L);
                    }
                });
            }
        });

        return tcs.getTask();
    }
}
