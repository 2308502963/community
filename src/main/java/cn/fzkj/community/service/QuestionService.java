package cn.fzkj.community.service;

import cn.fzkj.community.cache.TagCache;
import cn.fzkj.community.domain.Question;
import cn.fzkj.community.domain.QuestionExample;
import cn.fzkj.community.domain.User;
import cn.fzkj.community.dto.PageBean;
import cn.fzkj.community.dto.QuesQueryDTO;
import cn.fzkj.community.dto.QuestionDTO;
import cn.fzkj.community.dto.TagDTO;
import cn.fzkj.community.exception.CustomErrorCode;
import cn.fzkj.community.exception.CustomException;
import cn.fzkj.community.mapper.QuestionExtMapper;
import cn.fzkj.community.mapper.QuestionMapper;
import cn.fzkj.community.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    @Autowired(required = false) //required = false :有就注入，没有就跳过
    private QuestionMapper questionMapper;

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired(required = false)
    private QuestionExtMapper questionExtMapper;

    public void updateOrcreateQues(Question question) {
        if(question.getId()==null){
            //创建问题
            question.setGmtCreate(System.currentTimeMillis());
            question.setGmtModified(question.getGmtCreate());
            questionMapper.insertSelective(question);
        }else{
            //更新问题
            question.setGmtModified(System.currentTimeMillis());
            QuestionExample example = new QuestionExample();
            example.createCriteria()
                    .andIdEqualTo(question.getId());
            questionMapper.updateByExampleSelective(question, example);
        }
    }

    //查询所有的问题回显到index页面
    public PageBean<QuestionDTO> questionList(String search,Integer page) {
        if (StringUtils.isNotBlank(search)){
            String[] split = StringUtils.split(search, " ");
            search = Arrays.stream(split).collect(Collectors.joining("|"));
        }
        List<QuestionDTO> list = new ArrayList<>();
        PageBean<QuestionDTO> pagesinfo = new PageBean<>();
        pagesinfo.setPage(page);
        //1.设置limit
        Integer limit = 5;
        pagesinfo.setLimit(limit);
        //2.设置总记录数
        QuesQueryDTO quesQueryDTO = new QuesQueryDTO();
        quesQueryDTO.setSearch(search);
        Integer total = questionExtMapper.countBySearch(quesQueryDTO);
        pagesinfo.setTotal(total);
        //3.设置总的页数
        Integer totalPage;
        if(total % limit == 0){
            totalPage = total / limit;
        }else{
            totalPage = total / limit + 1;
        }
        pagesinfo.setTotalPage(totalPage);
        //4.设置页数的集合
        List<Integer> pages = new ArrayList<>();
        //如果总页数大于7，就产生7个
        //如果总页数小于7.就显示全部
        if(totalPage > 5){
            if (page - 2 <= 0){
                for(int i=1; i< 6; i++){
                    pages.add(i);
                }
            }
            else if (page + 2 > totalPage){
                for(int i=page-2; i< totalPage+1; i++){
                    pages.add(i);
                }
            }
            else{
                for(int i=page-2; i < page+3; i++){
                    pages.add(i);
                }
            }
        }else{
            for(int i=1; i<totalPage+1; i++){
                pages.add(i);
            }
        }
        pagesinfo.setPages(pages);
        //5.设置每页的数据集合
        page = (page-1)*limit;  //计算每页开始的位置
        QuestionExample questionExample = new QuestionExample();
        questionExample.setOrderByClause("gmt_create desc");
        quesQueryDTO.setPage(page);
        quesQueryDTO.setLimit(limit);
        List<Question> questions = questionExtMapper.selectBySearch(quesQueryDTO);
        for(Question question : questions){
            User user = userMapper.selectByPrimaryKey(question.getCreator());
            QuestionDTO questionDTO = new QuestionDTO();
            BeanUtils.copyProperties(question,questionDTO);
            questionDTO.setUser(user);
            list.add(questionDTO);
        }
        pagesinfo.setPageRecode(list);
        return pagesinfo;
    }

    //查询用户的问题集合返回
    public PageBean<QuestionDTO> findUserQuestions(Long id, Integer page) {
        List<QuestionDTO> list = new ArrayList<>();
        PageBean<QuestionDTO> pagesinfo = new PageBean<>();
        pagesinfo.setPage(page);
        //1.设置limit
        Integer limit = 5;
        pagesinfo.setLimit(limit);
        //2.设置总记录数,单个用户的所有问题数量
        QuestionExample example1 = new QuestionExample();
        example1.createCriteria().
                andCreatorEqualTo(id);
        Integer total = (int)questionMapper.countByExample(example1);
        pagesinfo.setTotal(total);
        //3.设置总的页数
        Integer totalPage;
        if(total % limit == 0){
            totalPage = total / limit;
        }else{
            totalPage = total / limit + 1;
        }
        pagesinfo.setTotalPage(totalPage);
        //4.设置页数的集合
        List<Integer> pages = new ArrayList<>();
        //如果总页数大于7，就产生7个
        //如果总页数小于7.就显示全部
        if(totalPage > 5){
            if (page - 2 <= 0){
                for(int i=1; i< 6; i++){
                    pages.add(i);
                }
            }
            else if (page + 2 > totalPage){
                for(int i=page-2; i< totalPage+1; i++){
                    pages.add(i);
                }
            }
            else{
                for(int i=page-2; i < page+3; i++){
                    pages.add(i);
                }
            }
        }else{
            for(int i=1; i<totalPage+1; i++){
                pages.add(i);
            }
        }
        pagesinfo.setPages(pages);
        //5.设置每页的数据集合
        page = (page-1)*limit;  //计算每页开始的位置
        QuestionExample example = new QuestionExample();
        example.createCriteria().
                andCreatorEqualTo(id);
        List<Question> questions = questionMapper.selectByExampleWithRowbounds(example, new RowBounds(page,limit));
        for(Question question : questions){
            User user = userMapper.selectByPrimaryKey(question.getCreator());
            QuestionDTO questionDTO = new QuestionDTO();
            BeanUtils.copyProperties(question,questionDTO);
            questionDTO.setUser(user);
            list.add(questionDTO);
        }
        pagesinfo.setPageRecode(list);
        return pagesinfo;
    }

    //通过问题的id查找
    public QuestionDTO findQuestionById(Long id) {
        Question question = questionMapper.selectByPrimaryKey(id);
        if(question == null){
            throw new CustomException(CustomErrorCode.QUESTION_NOT_FIND);
        }
        User user = userMapper.selectByPrimaryKey(question.getCreator());
        QuestionDTO questionDTO = new QuestionDTO();
        BeanUtils.copyProperties(question,questionDTO);
        questionDTO.setUser(user);
        return questionDTO;
    }

    //实现阅读数的增加
    public void incView(QuestionDTO questionDTO) {
//        questionDTO.setViewCount(1);
        questionExtMapper.incView(questionDTO);
    }

    // 查找出相关问题
    public List<QuestionDTO> selectRelated(QuestionDTO queryDTO) {
        if (StringUtils.isBlank(queryDTO.getTag())){
            return new ArrayList<>();
        }
        String newTag = StringUtils.replace(queryDTO.getTag(), ",", "|").replace("，","|");
        System.out.println("QuestionService lines 174: 分割之后的tag:"+newTag);
        Question question = new Question();
        question.setTag(newTag);
        question.setId(queryDTO.getId());
        List<Question> questions = questionExtMapper.selectRelated(question);

        // 使用lamda表达式将Question类型的数组转化为QuestionDTO类型的数组
        List<QuestionDTO> questionDTOS = questions.stream().map(q->{
            QuestionDTO questionDTO = new QuestionDTO();
            BeanUtils.copyProperties(q,questionDTO);
            return questionDTO;
        }).collect(Collectors.toList());
        return questionDTOS;
    }

    public void delQuesById(Long id) {
        questionMapper.deleteByPrimaryKey(id);
    }
}
