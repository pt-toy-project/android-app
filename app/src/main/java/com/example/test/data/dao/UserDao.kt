package com.example.test.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.test.data.entity.UserEntity

@Dao
interface UserDao {  /*Interface:"이런 기능이 있어야 한다"는 약속(명세)만 적어두는 문법(선언만)
                       Room이 이 Interface를 보고 실제 SQL 실행 코드를 자동으로 만들어줌
                       그래서 Dao는 우리가 직접 구현 하지 않아도 돌아감
                        */

    @Insert
    suspend fun insertUser(user: UserEntity)    /*DB작업 시간이 좀 걸릴 수 있어서 "이 작업이 끝날 떄까지 앱이 멈추지 않고 기다릴 수 있게 처리하는 방식"
                                                  suspend가 붙은 함수는 백그라운드에서 실행되다가, 결과가 나오면 알려주는 식으로 작동
                                                  */

    @Query("SELECT * FROM user WHERE user_id = :userId AND password = :password")
    suspend fun login(userId: String, password: String): UserEntity?

    @Query("SELECT * FROM user WHERE user_uid = :userUid")
    suspend fun getUserByUid(userUid: Int): UserEntity?

    @Query("UPDATE user SET coin_bal = :newBalance WHERE user_uid = :userUid")
    suspend fun updateCoinBalance(userUid: Int, newBalance: Int)

    @Query("UPDATE user SET current_hero_id = :heroId WHERE user_uid = :userUid")
    suspend fun updateCurrentHero(userUid: Int, heroId: Int)
}