include_recipe "hadoop::yarn"
libpath = File.expand_path '../../../kagent/libraries', __FILE__
require File.join(libpath, 'inifile')


yarn_service="nm"
yarn_command="nodemanager"

for script in node[:hadoop][:yarn][:scripts]
  template "#{node[:hadoop][:home]}/sbin/#{script}-#{yarn_service}.sh" do
    source "#{script}-#{yarn_service}.sh.erb"
    owner node[:hadoop][:yarn][:user]
    group node[:hadoop][:group]
    mode 0775
  end
end 

service yarn_command do
  supports :restart => true, :stop => true, :start => true, :status => true
  action :nothing
end

template "/etc/init.d/#{yarn_command}" do
  source "#{yarn_command}.erb"
  owner node[:hadoop][:yarn][:user]
  group node[:hadoop][:group]
  mode 0754
  notifies :enable, resources(:service => yarn_command)
  notifies :restart, "service[#{yarn_command}]"
end

if node[:kagent][:enabled] == "true" 
  kagent_config yarn_command do
    service "YARN"
    start_script "#{node[:hadoop][:home]}/sbin/root-start-#{yarn_service}.sh"
    stop_script "#{node[:hadoop][:home]}/sbin/stop-#{yarn_service}.sh"
    log_file "#{node[:hadoop][:logs_dir]}/yarn-#{node[:hadoop][:yarn][:user]}-#{yarn_command}-#{node['hostname']}.log"
    pid_file "#{node[:hadoop][:logs_dir]}/yarn-#{node[:hadoop][:yarn][:user]}-#{yarn_command}.pid"
    web_port node[:hadoop]["#{yarn_service}"][:http_port]
  end
end

hadoop_start "#{yarn_command}" do
end
