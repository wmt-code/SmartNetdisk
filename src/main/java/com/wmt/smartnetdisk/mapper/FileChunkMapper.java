package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.FileChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件分片 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {

    /**
     * 根据文件 MD5 查询所有分片
     *
     * @param fileMd5 文件MD5
     * @return 分片列表
     */
    @Select("SELECT * FROM file_chunk WHERE file_md5 = #{fileMd5} ORDER BY chunk_index")
    List<FileChunk> selectByFileMd5(String fileMd5);

    /**
     * 根据文件 MD5 查询已完成的分片索引列表
     *
     * @param fileMd5 文件MD5
     * @return 已完成的分片索引列表
     */
    @Select("SELECT chunk_index FROM file_chunk WHERE file_md5 = #{fileMd5} AND status = 1 ORDER BY chunk_index")
    List<Integer> selectUploadedChunkIndexes(String fileMd5);

    /**
     * 根据文件 MD5 删除所有分片记录
     *
     * @param fileMd5 文件MD5
     * @return 删除的记录数
     */
    @Delete("DELETE FROM file_chunk WHERE file_md5 = #{fileMd5}")
    int deleteByFileMd5(String fileMd5);

    /**
     * 统计指定文件已上传的分片数量
     *
     * @param fileMd5 文件MD5
     * @return 已上传的分片数量
     */
    @Select("SELECT COUNT(*) FROM file_chunk WHERE file_md5 = #{fileMd5} AND status = 1")
    int countUploadedChunks(String fileMd5);

    /**
     * 根据文件 MD5 和分片索引查询分片
     *
     * @param fileMd5    文件MD5
     * @param chunkIndex 分片索引
     * @return 分片信息，不存在则返回 null
     */
    @Select("SELECT * FROM file_chunk WHERE file_md5 = #{fileMd5} AND chunk_index = #{chunkIndex} LIMIT 1")
    FileChunk selectByMd5AndIndex(@Param("fileMd5") String fileMd5, @Param("chunkIndex") Integer chunkIndex);
}
