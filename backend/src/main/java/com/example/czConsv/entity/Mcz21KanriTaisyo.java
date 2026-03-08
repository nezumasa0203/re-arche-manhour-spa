package com.example.czConsv.entity;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/** M10: 管理対象マスタ（代行入力の管理対象者↔管理担当者） */
@Entity
@Table(name = "mcz21_kanri_taisyo")
public class Mcz21KanriTaisyo {

    @Id
    @Column(name = "kanritsy_esqid")
    public String kanritsyEsqid;

    @Id
    @Column(name = "kanritnt_esqid")
    public String kanritntEsqid;
}
