package com.example.ufabcirco.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.ufabcirco.ui.FilaFragment;
import com.example.ufabcirco.ui.TabelaFragment;
import com.example.ufabcirco.ui.GaleriaFragment;
import com.example.ufabcirco.ui.HorariosFragment;
import com.example.ufabcirco.ui.PlaceholderFragment;

public class SectionsPagerAdapter extends FragmentStateAdapter {

    private final String[] tabTitles = {"Fila", "Tabela", "Galeria", "Horários"};

    public SectionsPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FilaFragment();
            case 1:
                return TabelaFragment.newInstance();
            case 2:
                return HorariosFragment.newInstance();
            case 3:
                return GaleriaFragment.newInstance();
            default:
                return PlaceholderFragment.newInstance("Erro: Tab desconhecida");
        }
    }

    @Override
    public int getItemCount() {
        return tabTitles.length;
    }

    public String getPageTitle(int position) {
        return tabTitles[position];
    }
}