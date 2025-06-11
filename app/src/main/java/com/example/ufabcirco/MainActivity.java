package com.example.ufabcirco;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.ufabcirco.adapter.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.view_pager_main);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(sectionsPagerAdapter);

        viewPager.setUserInputEnabled(false);

        int[] tabIcons = {
                R.drawable.ic_fila,
                R.drawable.ic_tabela,
                R.drawable.ic_galeria
        };

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(sectionsPagerAdapter.getPageTitle(position));
            tab.setIcon(tabIcons[position]);
        }).attach();
    }
}