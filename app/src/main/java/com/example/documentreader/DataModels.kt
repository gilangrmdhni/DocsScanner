package com.example.documentreader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class KTPData(
    val nik: String,
    val nama: String,
    val tanggalLahir: String,
    val alamatLengkap: String,
    val rtRw: String,
    val provinsi: String,
    val kotaKabupaten: String,
    val kecamatan: String,
    val kelurahan: String
) : Parcelable

@Parcelize
data class PassportData(
    val nama: String,
    val tanggalLahir: String,
    val issuedDate: String,
    val expiredDate: String,
    val nomorPassport: String,
    val kodeNegara: String
) : Parcelable