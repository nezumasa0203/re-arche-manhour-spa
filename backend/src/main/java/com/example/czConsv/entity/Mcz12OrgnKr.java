package com.example.czConsv.entity;

import java.time.LocalDate;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M8: 組織階層マスタ */
@Entity
@Table(name = "mcz12_orgn_kr")
public class Mcz12OrgnKr {

    @Id
    @Column(name = "sikcd")
    public String sikcd;

    @Column(name = "endymd")
    public LocalDate endymd;

    @Column(name = "startymd")
    public LocalDate startymd;

    @Column(name = "krikaisocd")
    public String krikaisocd;

    @Column(name = "krijsikcd")
    public String krijsikcd;

    @Column(name = "egsyocd")
    public String egsyocd;

    @Column(name = "showno")
    public String showno;

    @Column(name = "jsikhyojijun")
    public String jsikhyojijun;

    @Column(name = "hyojikn")
    public String hyojikn;

    @Column(name = "hyojikj")
    public String hyojikj;

    @Column(name = "hyojiryaku")
    public String hyojiryaku;

    @Column(name = "iocd")
    public String iocd;

    @Column(name = "sikcdhonb")
    public String sikcdhonb;

    @Column(name = "honbhyojikn")
    public String honbhyojikn;

    @Column(name = "honbhyojikj")
    public String honbhyojikj;

    @Column(name = "honbhyojiryaku")
    public String honbhyojiryaku;

    @Column(name = "sikcdkyk")
    public String sikcdkyk;

    @Column(name = "kykhyojikn")
    public String kykhyojikn;

    @Column(name = "kykhyojikj")
    public String kykhyojikj;

    @Column(name = "kykhyojiryaku")
    public String kykhyojiryaku;

    @Column(name = "sikcdsitu")
    public String sikcdsitu;

    @Column(name = "situhyojikn")
    public String situhyojikn;

    @Column(name = "situhyojikj")
    public String situhyojikj;

    @Column(name = "situhyojiryaku")
    public String situhyojiryaku;

    @Column(name = "sikcdbu")
    public String sikcdbu;

    @Column(name = "buhyojikn")
    public String buhyojikn;

    @Column(name = "buhyojikj")
    public String buhyojikj;

    @Column(name = "buhyojiryaku")
    public String buhyojiryaku;

    @Column(name = "sikcdka")
    public String sikcdka;

    @Column(name = "kahyojikn")
    public String kahyojikn;

    @Column(name = "kahyojikj")
    public String kahyojikj;

    @Column(name = "kahyojiryaku")
    public String kahyojiryaku;
}
