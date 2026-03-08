package com.example.czConsv.entity;

import java.time.LocalDateTime;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** T3: グループキー（グルーピング集計） */
@Entity
@Table(name = "tcz14_grp_key")
public class Tcz14GrpKey {

    @Id
    @Column(name = "nendo_half")
    public String nendoHalf;

    @Id
    @Column(name = "sknno")
    public String sknno;

    @Column(name = "hshk_bunrui_code")
    public String hshkBunruiCode;

    @Column(name = "is_kyk_hs_tnt_bs_code")
    public String isKykHsTntBsCode;

    @Column(name = "kh_tnt_bs_code")
    public String khTntBsCode;

    @Column(name = "sys_kan_k_tbs_code")
    public String sysKanKTbsCode;

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
