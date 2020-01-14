package cn.ztuo.bitrade.dao;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cn.ztuo.bitrade.constant.CertifiedBusinessStatus;
import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.Member;

import java.util.Date;
import java.util.List;

public interface MemberDao extends BaseDao<Member> {

    List<Member> getAllByEmailEquals(String email);

    List<Member> getAllByUsernameEquals(String username);

    List<Member> getAllByMobilePhoneEquals(String phone);

    Member findByUsername(String username);

    Member findMemberByTokenAndTokenExpireTimeAfter(String token, Date date);

    Member findMemberByToken(String token);

    Member findMemberByMobilePhoneOrEmail(String phone, String email);

    int countByRegistrationTimeBetween(Date startTime, Date endTime);

    Member findMemberByPromotionCode(String code);

    Member findMemberByEmail(String email);

    Member findMemberByMobilePhone(String mobilePhone);

    List<Member> findAllByInviterId(Long id);



    Member findMemberById(long id);


    /*@Query("select new cn.ztuo.bitrade.dto.MemberDTO(member,memberWallet) from")*/

    @Query(value = "select m.username from member m where m.id = :id", nativeQuery = true)
    String findUserNameById(@Param("id") Long id);

    @Query(value = "select * from member where FIND_IN_SET(:id,genes)", nativeQuery = true)
    List<Member> findAllByPid(@Param("id") Long id);

    @Modifying
    @Query(value = "update Member set signInAbility = true ")
    void resetSignIn();


    @Query(value = "update Member set certified_business_status = :status where id in (:idList) and certified_business_status=2")
    void updateCertifiedBusinessStatusByIdList(@Param("idList")List<Long> idList, @Param("status")CertifiedBusinessStatus status);

    @Query(value ="select count(id) from member where date_format(registration_time,'%Y-%m-%d') = :date",nativeQuery = true)
    int getRegistrationNum(@Param("date")String date);

    @Query(value ="select count(id) from member where date_format(certified_business_check_time,'%Y-%m-%d') = :date",nativeQuery = true)
    int getBussinessNum(@Param("date")String date);

    //以前没有application_time,若以此方法，需手动更新 在添加application_time字段之前的会员的实名通过时间
    /*
        update member a , member_application b
        set a.application_time = b.update_time
        where b.audit_status = 2 and a.application_time is NULL
        and a.id = b.member_id;
     */
    /*@Query(value ="select count(id) from member where date_format(application_time,'%Y-%m-%d') = :date",nativeQuery = true)
    int getApplicationNum(@Param("date")String date);*/

    @Query(value ="select count(a.id) from member a , member_application b where a.id = b.member_id and b.audit_status = 2 and date_format(b.update_time,'%Y-%m-%d') = :date",nativeQuery = true)
    int getApplicationNum(@Param("date")String date);

    @Query("select min(a.registrationTime) as date from Member a")
    Date getStartRegistrationDate();

    List<Member> getAllByPromotionCodeEquals(String promotion);

    /**
     * 功能描述:查看用户的有效直推会员
     * @param:
     * @return:
     */
    @Query(value ="SELECT COUNT(1) FROM(\n" +
            "SELECT COUNT(1) FROM member_transaction AS a \n" +
            "LEFT JOIN member AS b\n" +
            "ON a.member_id=b.id\n" +
            "WHERE a.amount>=10 AND a.type=0 AND a.member_id in (SELECT id FROM member  WHERE inviter_id=:id AND real_name_status=2)\n" +
            "GROUP BY a.member_id\n" +
            ") AS c",nativeQuery = true)
    int effectiveMembership(@Param("id")long id);

    /**
     * 功能描述:查看用户上线所有有效会员信息及直推人数和团队人数
     * @param:
     * @return:
     */
    @Query(value = "SELECT a.id AS memberId,\n" +
            "\t(SELECT COUNT(1) FROM member AS b WHERE b.inviter_id=a.id) AS zhituinum,\n" +
            "\t(SELECT COUNT(1) FROM member AS c WHERE FIND_IN_SET(a.id,c.genes) )AS teamnum \n" +
            "FROM member_transaction AS d\n" +
            "LEFT JOIN member AS a\n" +
            "ON a.id= d.member_id\n" +
            "WHERE d.symbol='WBC'\n" +
            "AND a.id in(:genes )AND a.vip!=3\n" +
            "GROUP BY d.member_id ORDER BY d.member_id DESC ",nativeQuery = true)
    List<Member> upperMember(@Param("genes")List<String> genes);

    /**
     * 功能描述: 查看最靠近用户的3个VIP会员的
     * @param:
     * @return:
     */
    @Query(value = "SELECT * FROM member\n" +
            "WHERE id in(:genes) \n" +
            "AND vip!=0 AND vip IS NOT NULL\n" +
            "GROUP BY vip\n" +
            "ORDER BY id DESC ",nativeQuery = true)
    List<Member> vipMember(@Param("genes")List<String> genes);

    /**
     * 功能描述: 查看用户的直推人数
     * @param:
     * @return:
     */
    @Query(value = "SELECT COUNT(1) FROM  member_transaction AS a\n" +
            "LEFT JOIN member AS b\n" +
            "ON b.id= a.member_id\n" +
            "WHERE a.symbol='WBC' AND b.inviter_id=:id GROUP BY a.member_id",nativeQuery = true)
    Integer zhituinum(@Param("id")long id);

    /**
     * 功能描述: 查看用户团队的团队人数
     * @param:
     * @return:
     */
    @Query(value = "SELECT COUNT(1) FROM  member_transaction AS a\n" +
            "LEFT JOIN member AS b\n" +
            "ON b.id= a.member_id\n" +
            "WHERE a.symbol='WBC' AND FIND_IN_SET(:id,b.genes)) GROUP BY a.member_id",nativeQuery = true)
    Integer teamnum(@Param("id")long id);

    /**
     * 功能描述: 根据用户ID获取用户推荐人信息
     * @param:
     * @return:
     */
    @Query(value="SELECT * FROM member WHERE id =(\n" +
            "SELECT inviter_id FROM member WHERE id=:id)",nativeQuery = true)
    Member parentUser(@Param("id")long id);

    /**
     * 功能描述:查看用户所有的上级信息
     * @param:
     * @return:
     */
    @Query(value = "\n" +
            "SELECT * FROM member \n" +
            " WHERE FIND_IN_SET(:id,genes) \n" +
            "ORDER BY id DESC",nativeQuery = true)
    List<Member> genesMember(@Param("id")long id);

    /**
     * 功能描述:查看用户直推会员的信息
     * @param:
     * @return:
     */
    @Query(value = "SELECT * FROM member WHERE inviter_id=:id",nativeQuery = true)
    List<Member> zhiTuiMember(@Param("id")long id);

    /**
     * 功能描述: 查看用户多少层内的用户id
     * @param:
     * @return:
     */
    @Query(value = "SELECT id FROM member WHERE FIND_IN_SET(:id,genes) and generation-:generation<=:teamlayer",nativeQuery = true)
    List<Long> inLayer(@Param("id")long id,@Param("generation")Integer generation,@Param("teamlayer")Integer teamlayer);

    /**
     * 功能描述: 查看用户团队信息
     * @param:
     * @return:
     */
    @Query(value ="SELECT * FROM member WHERE FIND_IN_SET(:id,genes) ",nativeQuery = true)
    List<Member> teamUser(@Param("id")long id);

    /**
     * 功能描述: 查看系统中所有的会员信息
     * @param:
     * @return:
     */
    @Query(value ="SELECT * FROM member",nativeQuery = true)
    List<Member> memberAll();

    /**
     * 功能描述:查看用户所有的上级信息
     * @param:
     * @return:
     */
    @Query(value = "SELECT * FROM member WHERE id in(:ids) ORDER BY id DESC;",nativeQuery = true)
    List<Member> parentssMember(@Param("ids")List<String> ids);


    @Query(value = "SELECT COUNT(1) FROM member WHERE inviter_id=:id AND member_level=1 \n" +
            "AND datediff(registration_time,(SELECT registration_time FROM member WHERE id=:id)) <32",nativeQuery = true)
    Integer zhiTuiRenShu(@Param("id") long id);

    /***
     * 获取直推人数
     * @param memberId
     * @return
     */
    @Query(value = "select count(1) from member where inviter_id=:memberId",nativeQuery = true)
    Integer getCountDirectpush(@Param("memberId") long memberId);

    /***
     * 获取团队人数
     * @param memberId
     * @return
     */
    @Query(value = "select count(1) from member where FIND_IN_SET(:memberId,genes)",nativeQuery = true)
    Integer getCountTeam(@Param("memberId")long memberId);

    @Query(value = "select * from member where id > 9487",nativeQuery = true)
    List<Member> test();
}
