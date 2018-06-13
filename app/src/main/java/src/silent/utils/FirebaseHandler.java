package src.silent.utils;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import src.silent.utils.models.Artist;

public class FirebaseHandler {
    public static void insertArtist() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Artists");
        Artist artist = new Artist("myKey", "Test", "Test");
        databaseReference.child("myKey").setValue(artist);
    }

    public static void deleteArtist() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Artists")
                .child("myKey");
        databaseReference.removeValue();
    }
}
