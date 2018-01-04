package jenkins.plugins.logstash.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jenkins.plugins.logstash.persistence.AbstractLogstashIndexerDao;
import net.sf.json.JSONObject;

public class MemoryDao extends AbstractLogstashIndexerDao
{
    List<JSONObject> output = new ArrayList<>();

    public MemoryDao()
    {
        super();
    }

    @Override
    public void push(String data) throws IOException
    {
        JSONObject json = JSONObject.fromObject(data);
        output.add(json);
    }

    public List<JSONObject> getOutput()
    {
        return output;
    }

    @Override
    public String getDescription()
    {
      return "test";
    }
}