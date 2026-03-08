package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M4: サブシステムマスタ */
@Entity
@Table(name = "mav03_subsys")
public class Mav03Subsys {

    @Id
    @Column(name = "skbtcd")
    public String skbtcd;

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Id
    @Column(name = "subsysno")
    public String subsysno;

    @Column(name = "aplid")
    public String aplid;

    @Column(name = "subsys_mei")
    public String subsysMei;

    @Column(name = "subsys_mei_kn")
    public String subsysMeiKn;

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
