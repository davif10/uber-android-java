package com.davisilvaprojetos.uber.helper;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.davisilvaprojetos.uber.activity.PassageiroActivity;
import com.davisilvaprojetos.uber.activity.RequisicoesActivity;
import com.davisilvaprojetos.uber.config.ConfiguracaoFirebase;
import com.davisilvaprojetos.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UsuarioFirebase {

    public static FirebaseUser getUsuarioAtual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getFirebaseAutenticacao();
        return usuario.getCurrentUser();
    }

    public static boolean atualizarNomeUsuario(String nome){
        try{

            FirebaseUser  user = getUsuarioAtual();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful()){
                        System.out.println("Erro ao atualizar nome de perfil.");
                    }
                }
            });

            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void redirecionaUsuarioLogado(Activity activity){
        FirebaseUser user = getUsuarioAtual();
        if(user != null){
            DatabaseReference usuariosRef = ConfiguracaoFirebase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(getIdentificadorUsuario());
            usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    String tipoUsuario = usuario.getTipo();

                    if(tipoUsuario.equals("M")){
                        activity.startActivity(new Intent(activity, RequisicoesActivity.class));
                    }else{
                        activity.startActivity(new Intent(activity, PassageiroActivity.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

    }

    public static  String getIdentificadorUsuario(){
        return getUsuarioAtual().getUid();
    }
}
