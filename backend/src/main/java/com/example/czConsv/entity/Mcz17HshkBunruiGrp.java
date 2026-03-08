package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M7: 保守管轄分類グループマスタ */
@Entity
@Table(name = "mcz17_hshk_bunrui_grp")
public class Mcz17HshkBunruiGrp {

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Id
    @Column(name = "yukou_kaishiki")
    public String yukouKaishiki;

    @Id
    @Column(name = "yukou_syuryoki")
    public String yukouSyuryoki;

    @Column(name = "bunruicode")
    public String bunruicode;

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
