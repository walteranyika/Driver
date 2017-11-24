package io.clone.dereva.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import io.clone.dereva.common.Common;
import io.clone.dereva.models.Token;

/**
 * Created by walter on 11/22/17.
 */

public class MyFirebaseIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String token= FirebaseInstanceId.getInstance().getToken();
        updateTokenToServer(token);
    }

    private void updateTokenToServer(String refreshedToken) {
        FirebaseDatabase db=FirebaseDatabase.getInstance();
        DatabaseReference tokenRef=db.getReference(Common.TOKENS_TABLE);
        Token token=new Token(refreshedToken);
        if (FirebaseAuth.getInstance().getCurrentUser()!=null){
            tokenRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        }

    }
}
