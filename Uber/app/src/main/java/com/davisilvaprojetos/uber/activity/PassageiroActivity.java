package com.davisilvaprojetos.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.davisilvaprojetos.uber.config.ConfiguracaoFirebase;
import com.davisilvaprojetos.uber.helper.UsuarioFirebase;
import com.davisilvaprojetos.uber.model.Destino;
import com.davisilvaprojetos.uber.model.Requisicao;
import com.davisilvaprojetos.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.davisilvaprojetos.uber.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    /*
        Lat/Lon destino: -23.553992025417333, -46.652691603340635  (R. Frei Caneca, 569 - Consolação, São Paulo - SP, 01307-001)
        Lat/Lon passageiro ex:-23.55891853627467, -46.65934180861356
        Lat/Lon motorista(a caminho)
        Inicial: -23.563609548396446, -46.653730627810134
        Intermediária : -23.56310800163784, -46.65433144258068
        Final : -23.560187191295604, -46.65771102566495
        Encerramento intermediário: -23.55434895805354, -46.65435381665237
        Encerramento da corrida: -23.553602563131726, -46.65276969812975
     */
    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;

    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;
    private boolean uberChamado = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);
        inicializarComponentes();

        //Adiciona listener para status da requisição
        verificarStatusRequisicao();

    }

    private void verificarStatusRequisicao() {
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(usuarioLogado.getId());
        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Requisicao> lista = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    lista.add(ds.getValue(Requisicao.class));

                }

                Collections.reverse(lista);
                if (lista != null && lista.size() > 0) {
                    requisicao = lista.get(0);
                    switch (requisicao.getStatus()) {
                        case Requisicao.STATUS_AGUARDANDO:
                            linearLayoutDestino.setVisibility(View.GONE);
                            buttonChamarUber.setText("Cancelar Uber");
                            uberChamado = true;
                            break;
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Recuperar a localização do usuário
        recuperarLocalizacaoUsuario();
    }

    public void chamarUber(View view) {

        if (!uberChamado) {//Uber não foi chamado
            //Inicio
            String enderecoDestino = editDestino.getText().toString();
            if (!enderecoDestino.equals("") || enderecoDestino != null) {

                Address addressDestino = recuperarEndereco(enderecoDestino);
                if (addressDestino != null) {
                    Destino destino = new Destino();
                    destino.setCidade(addressDestino.getAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append("\nRua: " + destino.getRua());
                    mensagem.append("\nBairro: " + destino.getBairro());
                    mensagem.append("\nNúmero: " + destino.getNumero());
                    mensagem.append("\nCep: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereço!")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Salvar requisição
                                    salvarRequisicao(destino);
                                    uberChamado = true;
                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            } else {
                Toast.makeText(this, "Informe o endereço de destino", Toast.LENGTH_SHORT).show();
            }
            //Fim

        } else {
            //Cancelar a requisição
            uberChamado = false;
        }

    }

    private void salvarRequisicao(Destino destino) {
        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Cancelar Uber");
    }

    private Address recuperarEndereco(String endereco) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if (listaEnderecos != null && listaEnderecos.size() > 0) {
                Address address = listaEnderecos.get(0);

                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void recuperarLocalizacaoUsuario() {
        try {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    //Recuperar latitude e longitude
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    localPassageiro = new LatLng(latitude, longitude);
                    //Atualizar Geofire
                    UsuarioFirebase.atualizarDadosLocalizacao(latitude,longitude);

                    mMap.clear();
                    mMap.addMarker(
                            new MarkerOptions()
                                    .position(localPassageiro)
                                    .title("Meu local")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
                    );
                    mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(localPassageiro, 20)
                    );
                }

            };

            //Solicitar atualizações de localização
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        10000,
                        10,
                        locationListener
                );
            }

        } catch (AbstractMethodError e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void inicializarComponentes() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(toolbar);

        //Inicializar componentes
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarUber = findViewById(R.id.buttonChamarUber);
        //Configuracoes iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
}