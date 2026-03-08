package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M5: 対象システムマスタ */
@Entity
@Table(name = "mcz15_ts_sys")
public class Mcz15TsSys {

    @Id
    @Column(name = "tssknno")
    public String tssknno;

    @Id
    @Column(name = "tssubsysno")
    public String tssubsysno;

    @Id
    @Column(name = "yukou_syuryoki")
    public String yukouSyuryoki;

    @Column(name = "yukou_kaishiki")
    public String yukouKaishiki;

    @Column(name = "aplid")
    public String aplid;

    @Column(name = "tssysname")
    public String tssysname;

    @Column(name = "tssysname_kn")
    public String tssysnameKn;

    @Column(name = "tssubsysname")
    public String tssubsysname;

    @Column(name = "tssubsysname_kn")
    public String tssubsysnameKn;

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
