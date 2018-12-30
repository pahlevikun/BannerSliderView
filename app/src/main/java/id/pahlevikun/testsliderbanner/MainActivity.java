package id.pahlevikun.testsliderbanner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.pahlevikun.highlightbannerslider.adapter.BannerSliderAdapter;
import id.pahlevikun.highlightbannerslider.base.callback.BannerListener;
import id.pahlevikun.highlightbannerslider.widget.BannerSliderView;


public class MainActivity extends AppCompatActivity {

    BannerSliderAdapter bannerSliderAdapter;
    BannerSliderView bannerSliderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bannerSliderView = findViewById(R.id.recycler);

        final List<String> list = new ArrayList<>();
        list.add("http://img4.imgtn.bdimg.com/it/u=1794621527,1964098559&fm=27&gp=0.jpg");
        list.add("http://img0.imgtn.bdimg.com/it/u=1352823040,1166166164&fm=27&gp=0.jpg");
        list.add("http://img3.imgtn.bdimg.com/it/u=2293177440,3125900197&fm=27&gp=0.jpg");
        list.add("http://img3.imgtn.bdimg.com/it/u=3967183915,4078698000&fm=27&gp=0.jpg");
        list.add("http://img0.imgtn.bdimg.com/it/u=3184221534,2238244948&fm=27&gp=0.jpg");

        bannerSliderAdapter = new BannerSliderAdapter(list, new BannerListener<String>() {
            @Override
            public void onItemClick(int position, String data) {
                Toast.makeText(MainActivity.this, "Position " + position, Toast.LENGTH_SHORT).show();
            }
        });
        bannerSliderView.setAdapter(bannerSliderAdapter);
    }
}
