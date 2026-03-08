package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M3: システムマスタ */
@Entity
@Table(name = "mav01_sys")
public class Mav01Sys {

    @Id
    @Column(name = "skbtcd")
    public String skbtcd;

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Column(name = "sys_mei")
    public String sysMei;

    @Column(name = "sys_mei_kn")
    public String sysMeiKn;

    @Column(name = "yukou_kaishiki")
    public String yukouKaishiki;

    @Column(name = "yukou_syuryoki")
    public String yukouSyuryoki;

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
