package com.example.czConsv.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M9: 単価マスタ */
@Entity
@Table(name = "mcz24_tanka")
public class Mcz24Tanka {

    @Column(name = "yukou_kaishiki")
    public String yukouKaishiki;

    @Id
    @Column(name = "yukou_syuryoki")
    public String yukouSyuryoki;

    @Id
    @Column(name = "skbtcd")
    public String skbtcd;

    @Id
    @Column(name = "tanka_kbn")
    public String tankaKbn;

    @Column(name = "tanka")
    public BigDecimal tanka;

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
