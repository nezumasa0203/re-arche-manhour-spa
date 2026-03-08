package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M6: アプリ分類グループマスタ */
@Entity
@Table(name = "mcz03_apl_bunrui_grp")
public class Mcz03AplBunruiGrp {

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Id
    @Column(name = "yukou_kaishiki")
    public String yukouKaishiki;

    @Id
    @Column(name = "yukou_syuryoki")
    public String yukouSyuryoki;

    @Column(name = "aplbunruicode")
    public String aplbunruicode;

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
