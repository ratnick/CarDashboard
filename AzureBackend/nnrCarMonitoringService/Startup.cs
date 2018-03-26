using Microsoft.Owin;
using Owin;

[assembly: OwinStartup(typeof(nnrCarMonitoringService.Startup))]

namespace nnrCarMonitoringService
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            ConfigureMobileApp(app);
        }
    }
}