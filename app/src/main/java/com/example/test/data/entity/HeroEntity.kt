package com.example.test.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hero")     /*Annotation(@):코드 위에 붙어서 이 코드가 어떤 역할을 하는지
                                  Room라이브러리에 알려주는 표시 (@Entity:이 클래스는 DB테이블이다.)
                                  우리가 SQL을 직접 안 써도, Annotation 덕분에 Room이 알아서 SQL로 변환해줌
                                */
data class HeroEntity(  /*데이터 저장이 주 목적인 클래스
                          data를 붙이면 Kotlin이 자동으로 여러 편의 기능(값 비교, 출력) 제공
                         */
    @PrimaryKey
    val hero_id: Int,       //variable의 반대 value "한 번 정해지면 안 바뀌는 값"
    val hero_name: String,  //var은 나중에 값이 바뀔 수 있는 값(val 반대)
    val hero_desc: String,
    val hero_quote: String,
    val image_resource_name: String
)