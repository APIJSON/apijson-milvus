/*Copyright ©2024 APIJSON(https://github.com/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.milvus;

import apijson.NotNull;
import apijson.orm.SQLConfig;
import com.alibaba.fastjson.JSONObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.datayoo.moql.RecordSet;
import org.datayoo.moql.querier.milvus.MilvusQuerier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static apijson.orm.AbstractSQLExecutor.KEY_RAW_LIST;


/**
 * @author Lemon
 * @see DemoSQLExecutor 重写 execute 方法：
 *     \@Override
 *     public JSONObject execute(@NotNull SQLConfig<Long> config, boolean unknownType) throws Exception {
 *
 *         return MilvusUtil.execute(config, unknownType);
 *     }
 */
public class MilvusUtil {
    public static final String TAG = "MilvusUtil";

    public static JSONObject execute(@NotNull SQLConfig<Long> config, boolean unknownType) throws Exception {
        // 构建Milvus客户端
        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder().withUri(config.getDBUri()).build()
        );

        // 使用Milvus客户端创建Milvus查询器
        MilvusQuerier milvusQuerier = new MilvusQuerier(milvusClient);

        /*
            查询语句含义：从book集合中筛选数据，并返回col1,col2两个列。筛选条件为，当数据的col3列值为4，col4列值为'a','b','c'中的任意一
            个，且vec向量字段采用'L2'类型匹配，值为'[[1.0, 2.0, 3.0],[1.1,2.1,3.1]]'。另外，采用强一致性级别在10个单元内进行检索，取第11到第15，5条命中记录。
        */
        String sql = config.getSQL(false); //
//      String sql = "select id,userId,momentId,content,date from Comment where vMatch(vec, 'L2', '[[1]]') and consistencyLevel('STRONG')  limit 1,1";
        // 使用查询器执行sql语句，并返回查询结果
        RecordSet recordSet = milvusQuerier.query(sql);

//      int count = recordSet == null ? 0 : recordSet.getRecordsCount();
        List<Map<String, Object>> list = recordSet == null ? null : recordSet.getRecordsAsMaps();
//      RecordSetDefinition def = recordSet.getRecordSetDefinition();
//      List<ColumnDefinition> cols = def.getColumns();

//      List<Object[]> list = count <= 0 ? null : recordSet.getRecords();

        if (list == null || list.isEmpty()) {
            return new JSONObject(true);
        }

        List<JSONObject> nl = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);

            JSONObject obj = new JSONObject(map == null ? new HashMap<>() : map);
            // obj.put(col.getValue(), os[j]);
//          for (int j = 0; j < os.length; j++) {
//              ColumnDefinition col = cols.get(j);
//              obj.put(col.getValue(), os[j]);
//          }
            nl.add(obj);
        }

        JSONObject result = nl.get(0); // JSON.parseObject(list.get(0));
        if (nl.size() > 1) {
            result.put(KEY_RAW_LIST, nl);
        }

        return result;
    }

}
