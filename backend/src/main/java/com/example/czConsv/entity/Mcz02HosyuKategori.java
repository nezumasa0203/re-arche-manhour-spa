package com.example.czConsv.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M2: 保守カテゴリマスタ */
@Entity
@Table(name = "mcz02_hosyu_kategori")
public class Mcz02HosyuKategori {

    @Id
    @Column(name = "hs_kategori")
    public String hsKategori;

    @Id
    @Column(name = "yukou_kaishiki")
    public LocalDate yukouKaishiki;

    @Id
    @Column(name = "yukou_syuryoki")
    public LocalDate yukouSyuryoki;

    @Column(name = "hs_syubetu")
    public String hsSyubetu;

    @Column(name = "hs_unyou_kubun")
    public String hsUnyouKubun;

    @Column(name = "hs_kategori_mei")
    public String hsKategoriMei;

    @Column(name = "hs_kategori_naiyo")
    public String hsKategoriNaiyo;

    @Column(name = "initnt")
    public String initnt;

    @Column(name = "inidate")
    public LocalDateTime inidate;

    @Column(name = "updtnt")
    public String updtnt;

    @Column(name = "upddate")
    public LocalDateTime upddate;

    @Column(name = "updpgid")
    public String updpgid;

    @Column(name = "delflg")
    public String delflg;
}
