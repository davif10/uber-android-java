package com.davisilvaprojetos.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.davisilvaprojetos.uber.config.ConfiguracaoFirebase;
import com.davisilvaprojetos.uber.helper.UsuarioFirebase;
import com.davisilvaprojetos.uber.model.Requisicao;
import com.davisilvaprojetos.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.davisilvaprojetos.uber.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class CorridaActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private Button buttonAceitarCorrida;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private String statusRequisicao;
    private boolean requisicaoAtiva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida);
        inicializarComponentes();

        //Recupera dados do usuário
        if(getIntent().getExtras().containsKey("idRequisicao") && getIntent().getExtras().containsKey("motorista")){
            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();
        }

    }

    private void verificaStatusRequisicao(){
        DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child(idRequisicao);

        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Recupera requisicao
                requisicao = snapshot.getValue(Requisicao.class);
                if(requisicao != null){
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );
                    statusRequisicao = requisicao.getStatus();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    private void alteraInterfaceStatusRequisicao(String status){
        switch (status){
            case Requisicao.STATUS_AGUARDANDO:
                requisicaoAguardando();
                break;
            case Requisicao.STATUS_A_CAMINHO:
                requisicaoACaminho();
                break;
        }
    }

    private void requisicaoAguardando(){
        buttonAceitarCorrida.setText("Aceitar corrida");

        //Exibe marcador do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(localMotorista,20)
        );

    }

    private void requisicaoACaminho(){
        buttonAceitarCorrida.setText("A caminho do passageiro");

        //Exibe marcador do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //Exibe marcador do passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista,marcadorPassageiro);

        //Inicia monitoramento do motorista/passageiro
        iniciarMonitoramentoCorrida(passageiro,motorista);

    }

    private void iniciarMonitoramentoCorrida(Usuario p, Usuario m){
        //InicializarGeofire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);
        //Adiciona círculo no passageiro
        Circle circulo = mMap.addCircle(
                new CircleOptions()
                .center(localPassageiro)
                .radius(50)//em metros
                .fillColor(Color.argb(90,255,153,0))
                .strokeColor(Color.argb(190,255,152,0))
        );

        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localPassageiro.latitude,localPassageiro.longitude),
                0.05 //em KM (50m)
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(key.equals(motorista.getId())){
                    //Alterar status da requisição
                    requisicao.setStatus(Requisicao.STATUS_VIAGEM);
                    requisicao.atualizarStatus();
                    //Remove listener e o círculo
                    geoQuery.removeAllListeners();
                    circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,largura, altura,espacoInterno)
        );
    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo){
        if(marcadorMotorista != null){
            marcadorMotorista.remove();
        }

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );

    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo){
        if(marcadorPassageiro != null){
            marcadorPassageiro.remove();
        }

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Recuperar a localização do usuário
        recuperarLocalizacaoUsuario();
    }

    public void aceitarCorrida(View view){
        //Configura a requisicao
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);

        requisicao.atualizar();
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
                    localMotorista = new LatLng(latitude, longitude);
                    //Atualizar Geofire
                    UsuarioFirebase.atualizarDadosLocalizacao(latitude,longitude);

                    alteraInterfaceStatusRequisicao(statusRequisicao);

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
        }catch (AbstractMethodError e){
            System.out.println("Erro: "+ e.getMessage());
        }catch (Exception e){
            System.out.println("ERRO: "+ e.getMessage());
        }

    }

    private void inicializarComponentes() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar corrida");

        //Configurações iniciais
        buttonAceitarCorrida = findViewById(R.id.buttonAceitarCorrida);
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if(requisicaoAtiva){
            Toast.makeText(this, "Necessário encerrar a requisição atual!", Toast.LENGTH_SHORT).show();
        }else{
            Intent i = new Intent(CorridaActivity.this,RequisicoesActivity.class);
            startActivity(i);
        }
        return false;
    }
}