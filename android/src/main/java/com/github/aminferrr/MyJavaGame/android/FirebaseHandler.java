package com.github.aminferrr.MyJavaGame.android;

import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Простой обработчик Firebase.
 * Просто инициализирует сервисы, ничего не делает,
 * но создаёт структуру в проекте.
 */
public class FirebaseHandler {
    private static final String TAG = "FirebaseHandler";

    private FirebaseAnalytics analytics;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    public FirebaseHandler() {
        Log.d(TAG, "FirebaseHandler initialized");

        try {
            // Инициализация всех сервисов Firebase
            analytics = FirebaseAnalytics.getInstance(AndroidLauncher.context);
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();

            // Подписка на темы для уведомлений
            FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to all_devices topic");
                    } else {
                        Log.w(TAG, "Failed to subscribe to topic", task.getException());
                    }
                });

            Log.d(TAG, "All Firebase services initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase services", e);
        }
    }

    public FirebaseAnalytics getAnalytics() {
        return analytics;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }
}
